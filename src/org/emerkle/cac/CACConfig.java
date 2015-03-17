/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.emerkle.cac;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 *
 * @author emerkle
 */
class CACConfig {
    private static final String NAME = "name";
    private static final String LIBRARY = "library";
    private static final String SLOT = "slotListIndex";
    private static final char SPACE = ' ';
    private static final char EQUALS = '=';
    private static final String NL = System.getProperty("line.separator", "\n");
    
    private String name;
    private String library;
    private int slot = -1;
    
    private CACConfig() {
        super();
    }
    
    static CACConfig generateConfig(String lib) {
        if (lib == null) {
            throw new IllegalArgumentException("CAC Library location can NOT be NULL");
        }
        CACConfig config = new CACConfig();
        config.library = lib;
        return config;
    }
    
    ByteArrayInputStream getStreamedConfig() {
        // see if the library set is accessible
        if (library == null) {
            System.out.println("CAC Library not specified!");
            return null;
        }
        final File libFile = new File(library);
        if (libFile == null || !libFile.exists()) {
            System.out.println("Specified Library file does not exist: " + library);
            return null;
        }
        if (!libFile.isFile()) {
            System.out.println("Specified CAC Library file is not a regular file: " + library);
            return null;
        }
        if (!libFile.canRead()) {
            System.out.println("Specified CAC Library file is not readable: " + library);
            return null;
        }
        // library is good, generate the config file from the data
        if (name == null) {
            // use a generic name
            name = "Generated_CAC_Config";
        }
        String slotString = System.getProperty("cac.slot", "-1");
        try {
            slot = Integer.parseInt(slotString);
        } catch (Throwable t) {
            System.out.println("Invalid CAC slot specification: " + slotString);
            t.printStackTrace();
            return null;
        }
        if (slot < 0) {
            // default to the first one
            slot = 0;
        }
        StringBuilder builder = new StringBuilder();
        // build the config
        builder.append(NAME).append(SPACE).append(EQUALS).append(SPACE).append(name)
                .append(NL).append(LIBRARY).append(SPACE).append(EQUALS).append(SPACE).append(library)
                .append(NL).append(SLOT).append(SPACE).append(EQUALS).append(SPACE).append(slot);
        // return a byte array input stream of the bytes
        System.out.println(builder);
        return new ByteArrayInputStream(builder.toString().getBytes());
    }
}
