// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.pds.job;

import static javax.persistence.EnumType.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mercedesbenz.sechub.commons.model.LocalDateTimeDeserializer;
import com.mercedesbenz.sechub.commons.model.LocalDateTimeSerializer;

/**
 * Represents a PDS Job which contains information about ownership, related
 * sechub job and also state,configuration and last but not least the result of
 * the job.
 *
 * @author Albert Tregnaghi
 *
 */
@Entity
@Table(name = PDSJob.TABLE_NAME)
public class PDSJob {

    /* +-----------------------------------------------------------------------+ */
    /* +............................ SQL ......................................+ */
    /* +-----------------------------------------------------------------------+ */
    public static final String TABLE_NAME = "PDS_JOB";

    public static final String COLUMN_UUID = "UUID";

    public static final String COLUMN_SERVER_ID = "SERVER_ID";
    public static final String COLUMN_STATE = "STATE";
    public static final String COLUMN_OWNER = "OWNER";

    public static final String COLUMN_CREATED = "CREATED";
    public static final String COLUMN_STARTED = "STARTED";
    public static final String COLUMN_ENDED = "ENDED";

    public static final String COLUMN_CONFIGURATION = "CONFIGURATION";

    public static final String COLUMN_RESULT = "RESULT";

    public static final String COLUMN_ERROR_STREAM_TEXT = "ERROR_STREAM_TEXT";

    public static final String COLUMN_OUTPUT_STREAM_TEXT = "OUTPUT_STREAM_TEXT";

    public static final String COLUMN_MESSAGES = "MESSAGES";

    public static final String COLUMN_LAST_STREAM_TEXT_REFRESH_REQUEST = "LAST_STREAM_TEXT_REFRESH_REQUEST";
    public static final String COLUMN_LAST_STREAM_TEXT_UPDATE = "LAST_STREAM_TEXT_UPDATE";

    /* +-----------------------------------------------------------------------+ */
    /* +............................ JPQL .....................................+ */
    /* +-----------------------------------------------------------------------+ */
    public static final String CLASS_NAME = PDSJob.class.getSimpleName();

    public static final String PROPERTY_UUID = "uUID";
    public static final String PROPERTY_SERVER_ID = "serverId";

    public static final String PROPERTY_STATE = "state";
    public static final String PROPERTY_OWNER = "owner";

    public static final String PROPERTY_CREATED = "created";
    public static final String PROPERTY_STARTED = "started";
    public static final String PROPERTY_ENDED = "ended";

    public static final String PROPERTY_CONFIGURATION = "configuration";
    public static final String PROPERTY_RESULT = "result";

    public static final String QUERY_DELETE_JOB_OLDER_THAN = "DELETE FROM PDSJob j WHERE j." + PROPERTY_CREATED + " < :cleanTimeStamp";

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = COLUMN_UUID, updatable = false, nullable = false, columnDefinition = "UUID")
    UUID uUID;

    @Column(name = COLUMN_OWNER, nullable = false)
    String owner;

    /**
     * Server ID is used to give possibilty to use a shared database for multiple
     * PDS clusters. Members of cluster use the same server id, so scheduling etc.
     * is working well even when multiple PDS are running
     */
    @Column(name = COLUMN_SERVER_ID, nullable = false)
    String serverId;

    @Column(name = COLUMN_CREATED, nullable = false) // remark: we setup hibernate to use UTC settings - see
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime created;

    @Column(name = COLUMN_STARTED) // remark: we setup hibernate to use UTC settings - see application.properties
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime started;

    @Column(name = COLUMN_ENDED) // remark: we setup hibernate to use UTC settings - see application.properties
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime ended;

    @Column(name = COLUMN_LAST_STREAM_TEXT_REFRESH_REQUEST) // remark: we setup hibernate to use UTC settings - see application.properties
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime lastStreamTextRefreshRequest;

    @Column(name = COLUMN_LAST_STREAM_TEXT_UPDATE) // remark: we setup hibernate to use UTC settings - see application.properties
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime lastStreamTextUpdate;

    @Column(name = COLUMN_CONFIGURATION)
    String jsonConfiguration;

    @Column(name = COLUMN_RESULT)
    @Type(type = "text") // why not using @Lob, because hibernate/postgres issues. see
    // https://stackoverflow.com/questions/25094410/hibernate-error-while-persisting-text-datatype?noredirect=1#comment39048566_25094410
    String result;

    @Column(name = COLUMN_ERROR_STREAM_TEXT)
    @Type(type = "text") // see remarks on COLUMN_RESULT
    String errorStreamText;

    @Column(name = COLUMN_OUTPUT_STREAM_TEXT)
    @Type(type = "text") // see remarks on COLUMN_RESULT
    String outputStreamText;

    @Enumerated(STRING)
    @Column(name = COLUMN_STATE, nullable = false)
    PDSJobStatusState state = PDSJobStatusState.CREATED;

    @Version
    @Column(name = "VERSION")
    Integer version;

    @Column(name = COLUMN_MESSAGES)
    @Type(type = "text") // see remarks on COLUMN_RESULT
    String messages;

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setState(PDSJobStatusState executionResult) {
        this.state = executionResult;
    }

    public void setStarted(LocalDateTime started) {
        this.started = started;
    }

    public LocalDateTime getStarted() {
        return started;
    }

    public void setEnded(LocalDateTime ended) {
        this.ended = ended;
    }

    public LocalDateTime getEnded() {
        return ended;
    }

    public UUID getUUID() {
        return uUID;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public String getJsonConfiguration() {
        return jsonConfiguration;
    }

    public PDSJobStatusState getState() {
        return state;
    }

    public String getResult() {
        return result;
    }

    public LocalDateTime getLastStreamTextRefreshRequest() {
        return lastStreamTextRefreshRequest;
    }

    public LocalDateTime getLastStreamTextUpdate() {
        return lastStreamTextUpdate;
    }

    public String getOutputStreamText() {
        return outputStreamText;
    }

    public String getErrorStreamText() {
        return errorStreamText;
    }

    public String getMessages() {
        return messages;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uUID == null) ? 0 : uUID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PDSJob other = (PDSJob) obj;
        return Objects.equals(uUID, other.uUID);
    }

}
