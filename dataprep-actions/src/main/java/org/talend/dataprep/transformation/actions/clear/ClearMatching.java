// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.actions.clear;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.parameters.ParameterType;
import org.talend.dataprep.parameters.SelectParameter;
import org.talend.dataprep.transformation.actions.common.*;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

import java.util.List;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.talend.dataprep.transformation.actions.DataprepActionsBundle.choice;
import static org.talend.dataprep.transformation.actions.category.ActionCategory.DATA_CLEANSING;

/**
 * Clear cell when value is matching.
 */

@DataprepAction(AbstractActionMetadata.ACTION_BEAN_PREFIX + ClearMatching.ACTION_NAME)
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ClearMatching extends AbstractClear implements ColumnAction {

    /** the action name. */
    public static final String ACTION_NAME = "clear_matching"; //$NON-NLS-1$

    public static final String VALUE_PARAMETER = "matching_value"; //$NON-NLS-1$

    private final Type type;

    @Autowired
    private ReplaceOnValueHelper regexParametersHelper;

    @Autowired
    private ApplicationContext applicationContext;

    public ClearMatching() {
        this(Type.STRING);
    }

    public ClearMatching(Type type) {
        this.type = type;
    }

    /**
     * @see ActionMetadata#getName()
     */
    @Override
    public String getName() {
        return ACTION_NAME;
    }

    /**
     * @see ActionMetadata#getCategory()
     */
    @Override
    public String getCategory() {
        return DATA_CLEANSING.getDisplayName();
    }

    /**
     * @see ActionMetadata#acceptColumn(ColumnMetadata)
     */
    @Override
    public boolean acceptColumn(ColumnMetadata column) {
        return true;
    }

    @Override
    public List<Parameter> getParameters() {
        final List<Parameter> parameters = super.getParameters();
        if (this.type == Type.BOOLEAN) {
            parameters.add(SelectParameter.Builder.builder() //
                    .name(VALUE_PARAMETER) //
                    .item(TRUE.toString(), choice(TRUE.toString())) //
                    .item(FALSE.toString(), choice(FALSE.toString())) //
                    .build());
        } else {
            parameters.add(new Parameter(VALUE_PARAMETER, ParameterType.REGEX, //
                    StringUtils.EMPTY, false, false, StringUtils.EMPTY, getMessagesBundle()));
        }

        return parameters;
    }

    @Override
    public ClearMatching adapt(ColumnMetadata column) {
        if (column == null || !acceptColumn(column)) {
            return this;
        }
        return applicationContext.getBean(ClearMatching.class, Type.valueOf(column.getType().toUpperCase()));
    }

    @Override
    public boolean toClear(ColumnMetadata colMetadata, String value, ActionContext context) {
        Map<String, String> parameters = context.getParameters();
        String equalsValue = parameters.get(VALUE_PARAMETER);

        if (Type.get(colMetadata.getType()) == Type.BOOLEAN) { // for boolean we can accept True equalsIgnoreCase true
            return StringUtils.equalsIgnoreCase(value, equalsValue);
        } else {
            ReplaceOnValueHelper replaceOnValueHelper = regexParametersHelper.build(equalsValue, true);
            return replaceOnValueHelper.matches(value);
        }
    }

}
