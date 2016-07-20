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
package org.talend.dataprep.transformation.actions.math;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.parameters.ParameterType;
import org.talend.dataprep.parameters.SelectParameter;
import org.talend.dataprep.transformation.actions.common.ColumnAction;
import org.talend.dataprep.transformation.actions.common.OtherColumnParameters;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

import java.util.List;
import java.util.Map;

import static org.talend.dataprep.transformation.actions.DataprepActionsBundle.choice;
import static org.talend.dataprep.transformation.actions.common.OtherColumnParameters.*;

/**
 * Abstract Action for basic math action with one parameter (constant or an other column)
 */
public abstract class AbstractMathOneParameterAction extends AbstractMathAction implements ColumnAction {

    // olamy I would prefer to know which class is currently used but not sure checkstyle like that :P
    // private Logger logger = LoggerFactory.getLogger( getClass() );

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMathOneParameterAction.class);

    private static final String DEFAULT_VALUE_NAN = Integer.toString(Integer.MAX_VALUE);

    @Override
    public List<Parameter> getParameters() {
        List<Parameter> parameters = super.getParameters();

        parameters.add(SelectParameter.Builder.builder() //
                .name(MODE_PARAMETER) //
                .item(CONSTANT_MODE, choice(CONSTANT_MODE), new Parameter(CONSTANT_VALUE, ParameterType.STRING, StringUtils.EMPTY)) //
                .item(OTHER_COLUMN_MODE, choice(OTHER_COLUMN_MODE),
                        new Parameter(SELECTED_COLUMN_PARAMETER, ParameterType.COLUMN, //
                                StringUtils.EMPTY, false, false, //
                                StringUtils.EMPTY, getMessagesBundle())) //
                .defaultValue(CONSTANT_MODE) //
                .build());

        return parameters;
    }

    protected abstract String calculateResult(String columnValue, String parameter);

    @Override
    public void applyOnColumn(DataSetRow row, ActionContext context) {
        String columnId = context.getColumnId();
        String colValue = row.get(columnId);

        Map<String, String> parameters = context.getParameters();

        String mode = parameters.get(OtherColumnParameters.MODE_PARAMETER);

        String parameterValue;
        switch (mode) {
        case OtherColumnParameters.CONSTANT_MODE:
            parameterValue = parameters.get(CONSTANT_VALUE);
            break;
        case OtherColumnParameters.OTHER_COLUMN_MODE:
            String otherColId = parameters.get(SELECTED_COLUMN_PARAMETER);
            parameterValue = row.get(otherColId);
            break;
        default:
            throw new TDPException(CommonErrorCodes.BAD_ACTION_PARAMETER, //
                    ExceptionContext.build().put("paramName", OtherColumnParameters.CONSTANT_MODE));
        }

        String result = ERROR_RESULT;

        try {
            if (NumberUtils.isNumber(colValue) && NumberUtils.isNumber(parameterValue)) {
                result = calculateResult(colValue, parameterValue);
            } else {
                if (!NumberUtils.isNumber(parameterValue)) {
                    parameterValue = ExtractNumber.extractNumber(parameterValue, DEFAULT_VALUE_NAN);
                }
                if (!NumberUtils.isNumber(colValue)) {
                    colValue = ExtractNumber.extractNumber(colValue, DEFAULT_VALUE_NAN);
                }
                if (!StringUtils.equals(DEFAULT_VALUE_NAN, parameterValue) //
                        && !StringUtils.equals(DEFAULT_VALUE_NAN, colValue)) {
                    result = calculateResult(colValue, parameterValue);
                }
            }
        } catch (Exception e) {
            // ignore
            LOGGER.debug(e.getMessage(), e);
        }

        String newColumnId = context.column("result");
        row.set(newColumnId, result);
    }
}
