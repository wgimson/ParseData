package parsedata.ATNInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Vector;
import parsedata.DBObject;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.princeton.database.directory.entity.*;
import java.util.Iterator;
import com.princeton.database.global.entity.Keyword;

public class ParseDataATNInfo {
    private Vector<DBObject> DBOVector;
    private Vector<String> states;
    private String dataString;
    private StringBuffer businessInfoBuffer;
    private File dataFile;
    private BufferedReader dataReader;
    
    // Debugging
    private int matchCount;

    public ParseDataATNInfo() {
	    this.DBOVector = new Vector<DBObject>();
        this.businessInfoBuffer = new StringBuffer();
        this.states = new Vector<String>();
        this.populateStates();
        
        // Debugging 
        this.matchCount = 0;
    }
    
    private void populateStates() {
        this.states.add("ABUDHABI");
        this.states.add("AJMAN");
        this.states.add("DUBAI");
        this.states.add("FUJAIRAH");
        this.states.add("RASALKHAIMAH");
        this.states.add("SHARJAH");
        this.states.add("UMMALQUWAIN");
    }

    public Vector<DBObject> getDBOVector() {
	    return this.DBOVector;
    }
    
    public void parseString(LinkedList<String> filePathNames) {
        DBOVector.removeAll(DBOVector);
        ListIterator itr = filePathNames.listIterator();
        while (itr.hasNext()) {
            try {
                this.dataFile = new File((String) itr.next());

                // Debugging 
                System.out.println("File: " + dataFile.getPath() + " created");
                this.dataReader = new BufferedReader(new FileReader(dataFile));
            } catch (FileNotFoundException fnfe) {
                System.err.println("\nError: file not found.\n");
                continue;
            }
            try {
                while ((this.dataString = this.dataReader.readLine()) != null) {
                    if (this.dataString.matches(".*<div[\\s].*class=\"details_title\">.*")) {
                        this.businessInfoBuffer.append(dataString);
                        
                        // Debugging ///////////////////////////////////////////
                        this.matchCount++;
                        ////////////////////////////////////////////////////////
                        
                        this.getTablesIntoStringBuffer();
                        DBObject dbo = new DBObject();
                        boolean dboWasCreated = this.createDBO(dbo);
                        if (dboWasCreated) {
                            this.DBOVector.add(dbo);
                        }
                        this.businessInfoBuffer.setLength(0);
                    }
                }
            } catch (IOException ioe) {
                System.err.println("\nError: Input/Output exception.\n");
            }
        }
        this.removeIndianEntries(this.DBOVector);
    }
    
    private void getTablesIntoStringBuffer() {
        try {
            while (!(dataString = dataReader.readLine()).matches("<p class=" +
                    "\"pagibg\">.*")) {
                this.businessInfoBuffer.append(dataString);
            }
        } catch (IOException ioe) {
            System.err.println("\nError: Input/Output exception.\n");
        }
    }
    
    private boolean createDBO(DBObject dbo) {
        boolean orgNameAssigned = this.setOrganizationName(dbo);
        if (!orgNameAssigned) {
            return false;
        }
        this.setPhone(dbo);
        this.setFax(dbo);
        this.setPOBox(dbo);
        this.setState(dbo);
        this.setCity(dbo);
        this.setStreetAddress(dbo);
        //this.setCrossStreet(dbo);
        this.setSpecialty(dbo);
        this.setIndustries(dbo);
        this.setKeywords(dbo);
        dbo.setCountry("UAE");
 
        // Debugging
        return true;
    }
    
    private boolean setOrganizationName(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedOrganizationNameArray = 
                businessInfoString.split("<div[\\s]*class=\"name\">");
        if (unparsedOrganizationNameArray.length > 1) {
            String[] parsedOrganizationNameArray = 
                    unparsedOrganizationNameArray[1].split("</div>");
            dbo.setOrganizationName(this.formatOrganizationName(parsedOrganizationNameArray[0]));
            return true;
        }
        return false;
    }
    
    private String formatOrganizationName(String orgName) {
        orgName = orgName.replaceAll("[\\W]", " ");
        orgName = orgName.replaceAll("[\\s]+", " ");
        return orgName;
    }
    
    private void setPhone(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedPhoneArray = 
                businessInfoString.split("<span>Phone</span><span[\\s]*class=\"det\">");
        if (unparsedPhoneArray.length > 1) {
            String[] parsedPhoneArray = unparsedPhoneArray[1].split("</span>");
            if (this.isLegitPhoneOrFax(parsedPhoneArray[0])) {
                dbo.setPhone1(parsedPhoneArray[0]);
            }
        }
    }
    
    private void setFax(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedFaxArray = 
                businessInfoString.split("<span>Fax</span><span[\\s]*class=\"det\">");
        if (unparsedFaxArray.length > 1) {
            String[] parsedFaxArray = unparsedFaxArray[1].split("</span>");
            if (this.isLegitPhoneOrFax(parsedFaxArray[0])) {
                 dbo.setFax(parsedFaxArray[0]);
            }
        }
    }
    
    private boolean isLegitPhoneOrFax(String phoneOrFax) {
        if (phoneOrFax.matches("[\\d+-]*")) {
            return true;
        } else {
            return false;
        }
    }
    
    private void setPOBox(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedPOBoxArray = 
                businessInfoString.split("<span>Box No.</span><span[\\s]" +
                "*class=\"det\">");
        if (unparsedPOBoxArray.length > 1) {
            String[] parsedPOBoxArray = unparsedPOBoxArray[1].split("</span>");
            if (this.isLegitPOBox(parsedPOBoxArray[0])) {
                parsedPOBoxArray[0] = parsedPOBoxArray[0].replace("[^\\d]", "");
                dbo.setUnit("P.O. Box " + parsedPOBoxArray[0]);
            }
        }
    }
    
    private boolean isLegitPOBox(String po) {
        Pattern poPatt = Pattern.compile("(.*)([pP][\\.]?[oO][\\.]?([bB][oO][xX])?)?"
                + "([\\d]*)(.*)"); 
        Matcher poMatch = poPatt.matcher(po);
        if (poMatch.matches()) {
            return true;
        } else {
            return false;
        }
    }
    
    private void setStreetAddress(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedStreetAddressArray = businessInfoString.split("<span>" 
                +"Location</span><span[\\s]*class=\"det\">");
        if (unparsedStreetAddressArray.length > 1) {
            String[] parsedStreetAddressArray =
                    unparsedStreetAddressArray[1].split("</span>");
            if (this.isLegitStreetAddress(parsedStreetAddressArray[0])) {
                dbo.setStreetName(parsedStreetAddressArray[0]);
            }
        }
    }
    
   private boolean isLegitStreetAddress(String addr) {
       if (addr.matches("&nbsp;")) {
           return false;
       } else {
           return true;
       }
   }
   
   private  void setSpecialty(DBObject dbo) {
       String businessInfoString = this.businessInfoBuffer.toString();
       String[] unparsedSpecialtyArray = 
               businessInfoString.split("<div[\\s]*class=\"profileBody\">");
       if (unparsedSpecialtyArray.length > 1) {
           String[] parsedSpecialtyArray = unparsedSpecialtyArray[1].split("<p>");
           parsedSpecialtyArray = parsedSpecialtyArray[1].split("</p>");
           if (this.isLegitSpecialty(parsedSpecialtyArray[0])) {
               parsedSpecialtyArray[0].replaceAll("<.*>", "");
               dbo.setSpecialty(parsedSpecialtyArray[0]);
           }
       }
   }
   
   private boolean isLegitSpecialty(String specialty) {
       if (specialty.matches(".*&nbsp;.*")) {
           return false;
       } else {
           return true;
       }
   }
   
   private void setIndustries(DBObject dbo) {
       String businessInfoString = this.businessInfoBuffer.toString();
       String[] unparsedIndustriesArray = 
               businessInfoString.split("See[\\s]*all[\\s]*companies[\\s]*under[\\s]*");
       if (unparsedIndustriesArray.length > 1) {
               Vector<Industry> inds = 
                       this.createIndustryVector(unparsedIndustriesArray);
               dbo.setIndustries(inds);
       }
   }
   
   private boolean isLegitIndustry(String indName, Vector<Industry> inds) {
       for (int i = 0; i < inds.size(); i++) {
           if (inds.get(i).getName().matches(indName)) {
               return false;
           }
       }
       if (indName.matches(".*&nbsp;.*")) {
           return false;
       } else {
           return true;
       }
   }
   
   private Vector<Industry> createIndustryVector(String[] unparsedInds) {
       Vector<Industry> inds = new Vector<Industry>();
       for (int i = 1; i < unparsedInds.length; i++) {
           String[] parsedIndName = unparsedInds[i].split("\">");
           if (this.isLegitIndustry(parsedIndName[0], inds)) {
               Industry ind = new Industry();
               ind.setName(parsedIndName[0]);
               inds.add(ind);
           }
       }
       return inds;
   }
   
   private void setKeywords(DBObject dbo) {
       String businessInfoString = this.businessInfoBuffer.toString();
       if (!businessInfoString.matches(".*No[\\s]*keywords[\\s]*to[\\s]*display[\\s]*.*")) {
           String[] unparsedKeywords = businessInfoString.split("<div[\\s]*class=\"keywordsList\">");
           if (unparsedKeywords.length > 1) {
               String[] parsedKeywords = unparsedKeywords[1].split("</div>");
               Vector<Keyword> keys = this.createKeywordsVector(parsedKeywords[0]);
               dbo.setKeywords(keys);
           }
       }
   }
   
   private boolean isLegitKeyword(String keyword, Vector<Keyword> keys) {
       for (int i = 0; i < keys.size(); i++) {
           keyword = keyword.replaceAll("^[\\W]", "");
           keyword = keyword.replaceAll("[\\W]$", "");
           if (keys.get(i).getKeyword().matches(keyword)) {
               return false;
           }
       }
       if (keyword.matches(".*&nbsp;.*") || keyword.matches("")) {
           return false;
       } else {
           return true;
       }
   }
   
   private Vector<Keyword> createKeywordsVector(String keywords) {
       Vector<Keyword> keys = new Vector<Keyword>();
       String[] keywordsArray = keywords.split(",");
       for (int i = 0; i < keywordsArray.length; i++) {
           if (this.isLegitKeyword(keywordsArray[i], keys)) {
               Keyword key = new Keyword();
               key.setKeyword(keywordsArray[i].trim());
               keys.add(key);
           }
       }
       return keys;
   }
   
   private void setState(DBObject dbo) {
       String businessInfoString = this.businessInfoBuffer.toString();
       String[] unparsedStateArray = 
               businessInfoString.split("<span>Area</span><span[\\s]*class=\"det\">");
       if (unparsedStateArray.length > 1) {
           String[] parsedStateArray = unparsedStateArray[1].split("</span>");
           System.out.println();
            if (this.isLegitState(parsedStateArray[0])) {
               dbo.setState(parsedStateArray[0].trim());
           }
       }
   }
   
   private boolean isLegitState(String state) {
       for (int i = 0; i < this.states.size(); i++) {
           String formattedState = state.replaceAll("[\\W]", "");
           formattedState = formattedState.toUpperCase();
           if (formattedState.equals(states.get(i))) {
               return true;
           }
       }
       return false;
   }
   
   private void setCity(DBObject dbo) {
       String businessInfoString = this.businessInfoBuffer.toString();
       String[] unparsedCityArray = 
               businessInfoString.split("<span>Area</span><span[\\s]*class=\"det\">");
       if (unparsedCityArray.length > 1) {
           String[] parsedCityArray = unparsedCityArray[1].split("</span>");
           for (int i = 0; i < this.states.size(); i++) {
               String city = parsedCityArray[0].replaceAll(this.states.get(i), "");
               if (this.isLegitCity(city)) {
                   dbo.setCity(city);
               }
           }
       }
   }
   
   private boolean isLegitCity(String city) {
       if (city.matches("[\\W]") || city.matches("")) {
           return false;
       } else {
           return true;
       }
   }
   
   private void removeIndianEntries(Vector<DBObject> dboVect) {
       for (int i = 0; i < dboVect.size(); i++) {
           DBObject dbo =dboVect.get(i);
           String formattedCity = dbo.getCity().replaceAll("[\\W]", "");
           formattedCity = formattedCity.toUpperCase();
           if (formattedCity.equals("INDIA")) {
               dboVect.remove(dbo);
           }
       }
   }
}
