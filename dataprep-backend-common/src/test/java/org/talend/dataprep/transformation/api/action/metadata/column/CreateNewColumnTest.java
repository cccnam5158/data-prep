package org.talend.dataprep.transformation.api.action.metadata.column;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.talend.dataprep.api.dataset.ColumnMetadata.Builder.column;
import static org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils.getColumn;
import static org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils.getRow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.transformation.api.action.ActionTestWorkbench;
import org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils;
import org.talend.dataprep.transformation.api.action.metadata.category.ActionCategory;
import org.talend.dataprep.transformation.api.action.metadata.common.ImplicitParameters;

public class CreateNewColumnTest {

    /** The action to test. */
    private CreateNewColumn action;

    private Map<String, String> parameters;

    @Before
    public void init() throws IOException {
        action = new CreateNewColumn();
        parameters = ActionMetadataTestUtils.parseParameters(CreateNewColumnTest.class.getResourceAsStream("createNewColumnAction.json"));
    }

    @Test
    public void testAdapt() throws Exception {
        assertThat(action.adapt((ColumnMetadata) null), is(action));
        ColumnMetadata column = column().name("myColumn").id(0).type(Type.STRING).build();
        assertThat(action.adapt(column), is(action));
    }

    @Test
    public void testCategory() throws Exception {
        assertThat(action.getCategory(), is(ActionCategory.COLUMN_METADATA.getDisplayName()));
    }

    @Test
    public void should_copy_row_constant() {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0000", "lorem bacon");
        values.put("0001", "Bacon ipsum");
        values.put("0002", "01/01/2015");
        final DataSetRow row = new DataSetRow(values);

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "lorem bacon");
        expectedValues.put("0001", "Bacon ipsum");
        expectedValues.put("0003", "tagada");
        expectedValues.put("0002", "01/01/2015");

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void should_copy_row_empty() {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0000", "lorem bacon");
        values.put("0001", "Bacon ipsum");
        values.put("0002", "01/01/2015");
        final DataSetRow row = new DataSetRow(values);

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "lorem bacon");
        expectedValues.put("0001", "Bacon ipsum");
        expectedValues.put("0003", "");
        expectedValues.put("0002", "01/01/2015");

        parameters.put(CreateNewColumn.MODE_PARAMETER, CreateNewColumn.EMPTY_MODE);

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void should_copy_row_other_column() {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0000", "lorem bacon");
        values.put("0001", "Bacon ipsum");
        values.put("0002", "01/01/2015");
        final DataSetRow row = new DataSetRow(values);

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "lorem bacon");
        expectedValues.put("0001", "Bacon ipsum");
        expectedValues.put("0003", "Bacon ipsum");
        expectedValues.put("0002", "01/01/2015");

        parameters.put(CreateNewColumn.MODE_PARAMETER, CreateNewColumn.COLUMN_MODE);
        parameters.put(CreateNewColumn.SELECTED_COLUMN_PARAMETER, "0001");

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test(expected = TDPException.class)
    public void should_do_nothing_without_any_parameters() {
        // given
        DataSetRow row = getRow("first", "second", "Done !");
        parameters.put(ImplicitParameters.COLUMN_ID.getKey().toLowerCase(), "0000");
        parameters.remove(CreateNewColumn.MODE_PARAMETER);

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());
    }

    @Test(expected = TDPException.class)
    public void should_do_nothing_with_wrong_parameters_1() {
        // given
        DataSetRow row = getRow("first", "second", "Done !");
        parameters.put(ImplicitParameters.COLUMN_ID.getKey().toLowerCase(), "0000");
        parameters.remove(CreateNewColumn.DEFAULT_VALUE_PARAMETER);

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());
    }

    @Test(expected = TDPException.class)
    public void should_do_nothing_with_wrong_parameters_2() {
        // given
        DataSetRow row = getRow("first", "second", "Done !");
        parameters.put(ImplicitParameters.COLUMN_ID.getKey().toLowerCase(), "0000");
        parameters.remove(CreateNewColumn.DEFAULT_VALUE_PARAMETER);

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());
    }


    @Test(expected = TDPException.class)
    public void should_do_nothing_with_wrong_parameters_3() {
        // given
        DataSetRow row = getRow("first", "second", "Done !");
        parameters.put(ImplicitParameters.COLUMN_ID.getKey().toLowerCase(), "0000");
        parameters.put(CreateNewColumn.MODE_PARAMETER, CreateNewColumn.COLUMN_MODE);

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());
    }

    @Test(expected = TDPException.class)
    public void should_do_nothing_with_wrong_parameters_4() {
        // given
        DataSetRow row = getRow("first", "second", "Done !");
        parameters.put(ImplicitParameters.COLUMN_ID.getKey().toLowerCase(), "0000");
        parameters.put(CreateNewColumn.MODE_PARAMETER, CreateNewColumn.COLUMN_MODE);
        parameters.put(CreateNewColumn.SELECTED_COLUMN_PARAMETER, "0009");

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());
    }

    @Test
    public void should_accept_column() {
        assertTrue(action.acceptColumn(getColumn(Type.ANY)));
    }

}