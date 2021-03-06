package org.talend.dataprep.dataset.event;

import org.springframework.context.ApplicationEvent;
import org.talend.dataprep.api.dataset.DataSetMetadata;

/**
 * An event to indicate a data set content has been changed.
 */
public class DataSetRawContentUpdateEvent extends ApplicationEvent {

    public DataSetRawContentUpdateEvent(DataSetMetadata source) {
        super(source);
    }

    @Override
    public DataSetMetadata getSource() {
        return (DataSetMetadata) super.getSource();
    }
}
