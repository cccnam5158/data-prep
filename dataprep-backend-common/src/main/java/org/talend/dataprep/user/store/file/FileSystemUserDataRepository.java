package org.talend.dataprep.user.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.user.UserData;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.user.store.UserDataRepository;

/**
 * User data repository implementation backed on the file system.
 */
@Component
@ConditionalOnProperty(name = "user.data.store", havingValue = "file")
public class FileSystemUserDataRepository implements UserDataRepository {

    /** This class' logger. */
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemUserDataRepository.class);

    /** Where to store the dataset metadata */
    @Value("${user.data.store.file.location}")
    private String storeLocation;

    /** The dataprep ready jackson builder. */
    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    /**
     * Make sure the root folder is there.
     */
    @PostConstruct
    private void init() {
        new File(storeLocation).mkdirs();
    }

    /**
     * @see UserDataRepository#get(String)
     */
    @Override
    public UserData get(String userId) {

        final File inputFile = getFile(userId);
        if (!inputFile.exists()) {
            LOG.debug("user data #{} not found in file system", userId);
            return null;
        }

        try (GZIPInputStream input = new GZIPInputStream(new FileInputStream(inputFile))) {
            return builder.build().readerFor(UserData.class).readValue(input);
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNABLE_TO_READ_USER_DATA, e, ExceptionContext.build().put("id", userId));
        }
    }

    /**
     * @see UserDataRepository#save(UserData)
     */
    @Override
    public void save(UserData userData) {
        // defensive programming
        if (userData == null) {
            LOG.warn("cannot save a null user data.");
            return;
        }
        final File outputFile = getFile(userData.getUserId());

        try (GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(outputFile))) {
            builder.build().writer().writeValue(output, userData);
        } catch (IOException e) {
            LOG.error("Error saving {}", userData, e);
            throw new TDPException(CommonErrorCodes.UNABLE_TO_SAVE_USER_DATA, e);
        }
        LOG.debug("user data #{} saved", userData.getUserId());
    }

    /**
     * @see UserDataRepository#remove(String)
     */
    @Override
    public void remove(String userId) {
        final File userDataFile = getFile(userId);
        if (userDataFile.exists() && !isNFSFile(userDataFile)) {
            userDataFile.delete();
        }
        LOG.debug("user data {} successfully deleted", userId);
    }

    /**
     * @see UserDataRepository#clear()
     */
    @Override
    public void clear() {
        final File rootFolder = new File(storeLocation);
        final File[] files = rootFolder.listFiles();
        for (File file : files) {
            if (!isNFSFile(file)) {
                file.delete();
            }
        }
        LOG.debug("user data repository cleared");
    }

    /**
     * Return the file that matches the given metadata user id.
     *
     * @param userId the muser id.
     * @return the file where to read/write the user.
     */
    private File getFile(String userId) {
        return new File(storeLocation + '/' + userId);
    }

    /**
     * Specific OS NFS files must be not be dealt with.
     * 
     * @param file the file to look at.
     * @return True if the given file is OS NFS specific (starts with ".nfs")
     */
    private boolean isNFSFile(File file) {
        return file.getName().startsWith(".nfs");
    }

}
