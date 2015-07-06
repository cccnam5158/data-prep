package org.talend.dataprep.transformation.api.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.exception.CommonErrorCodes;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;
import org.talend.dataprep.transformation.api.action.metadata.ActionMetadata;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parse the actions a dataset and prepare the closures to apply.
 */
@Component
public class ActionParser implements BeanFactoryAware {

    private BeanFactory beanFactory;

    /**
     * Return the parsed actions ready to be run.
     *
     * @param actions the actions to be parsed as string.
     * @return the parsed actions.
     */
    public ParsedActions parse(String actions) {
        if (actions == null) {
            // Actions cannot be null (but can be empty string for no op actions).
            throw new IllegalArgumentException("Actions parameter can not be null.");
        }
        try {
            ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            String content = actions.trim();
            // no op
            if (content.isEmpty()) {
                //@formatter:off
                return new ParsedActions((row, context) -> {}, (rowMetadata, context) -> {});
                //@formatter:on
            }
            JsonNode node = mapper.readTree(content);
            Iterator<JsonNode> elements = node.elements();
            if (elements.hasNext()) {
                JsonNode root = elements.next();
                if (!root.isArray()) {
                    throw new IllegalArgumentException("'actions' element should contain an array of 'action' elements.");
                }
                List<BiConsumer<DataSetRow, TransformationContext>> parsedRowActions = new ArrayList<>();
                List<BiConsumer<RowMetadata, TransformationContext>> parsedMetadataActions = new ArrayList<>();

                Iterator<JsonNode> actionNodes = root.elements();
                while (actionNodes.hasNext()) {
                    JsonNode actionNode = actionNodes.next();
                    String actionType = actionNode.get("action").textValue().toLowerCase(); //$NON-NLS-1$
                    ActionMetadata currentAction;
                    // look for the appropriate action in the spring bean registry according to its type name
                    currentAction = beanFactory.getBean(ActionMetadata.ACTION_BEAN_PREFIX + actionType, ActionMetadata.class);
                    if (currentAction == null) {
                        throw new NotImplementedException("No support for action '" + actionType + "'."); //$NON-NLS-2$
                    }// else we got the action so keep going.

                    // parse the parameters
                    Iterator<Map.Entry<String, JsonNode>> parameters = actionNode.get("parameters").fields(); //$NON-NLS-1$
                    Map<String, String> parsedParameters = currentAction.parseParameters(parameters);
                    parsedRowActions.add(currentAction.create(parsedParameters));
                    parsedMetadataActions.add(currentAction.createMetadataClosure(parsedParameters));
                }

                // put all the row actions into a single consumer
                BiConsumer<DataSetRow, TransformationContext> rowConsumer = (row, context) -> {
                    for (BiConsumer<DataSetRow, TransformationContext> parsedAction : parsedRowActions) {
                        parsedAction.accept(row, context);
                    }
                };

                // as well as the metadata consumers
                BiConsumer<RowMetadata, TransformationContext> metadataConsumer = (rowMetadata, context) -> {
                    for (BiConsumer<RowMetadata, TransformationContext> metadataAction : parsedMetadataActions) {
                        metadataAction.accept(rowMetadata, context);
                    }
                };

                return new ParsedActions(rowConsumer, metadataConsumer);

            } else {
                // Should not happen, but no action means no op.
                //@formatter:off
                return new ParsedActions((row, context) -> {}, (rowMetadata, context) -> {});
                //@formatter:on
            }
        } catch (Exception e) {
            throw new TDPException(CommonErrorCodes.UNABLE_TO_PARSE_ACTIONS, e);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
}
