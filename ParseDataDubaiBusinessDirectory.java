package parsedata.dubaiBusinessDirectory;

import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.File;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.List;
import com.princeton.database.directory.entity.Industry;
import parsedata.DBObject;

public class ParseDataDubaiBusinesseDirectory {
	
    private Vector<DBObject> DBOVector;
    private HashMap<String, String> cities;
    private String dataString;
    private StringBuffer businessInfoBuffer;
    private File dataFile;
    private BufferedReader dataReader;

    public ParseDataDubaiBusinesseDirectory() {
	    this.DBOVector = new Vector<DBObject>();
        this.businessInfoBuffer = new StringBuffer();
        this.cities = new HashMap();
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
                System.out.println("File: " + dataFile.getPath() + "created");
                this.dataReader = new BufferedReader(new FileReader(dataFile));
            } catch (FileNotFoundException fnfe) {
                System.err.println("\nError: file not found.\n");
                continue;
            }

            try {
                while ((this.dataString = this.dataReader.readLine()) != null) {
                        if (this.dataString.matches(".*<table\\s*width=\"100%\">.*")) {
                            this.businessInfoBuffer.append(dataString);
                            this.getTableIntoStringBuffer();
                            DBObject dbo = new DBObject();
                            boolean dboWasCreated = 
                                    createDBO(dbo);
                            if (dboWasCreated) {
                                boolean dboWasAddedToVector =
                                        this.addDBOToVector(dbo);
                                
                                // Debugging
                                if (dbo.getCountry() == null) {
                                    System.out.println("\n\ndbo: " + dbo.getOrganizationName() +
                                            " has no country\n\n");
                                }
                                if (!dboWasAddedToVector) {
                                    return;
                                }
                            }
                            this.businessInfoBuffer.setLength(0);
                        }
                }
            } catch (IOException ioe) {
                System.err.println("\nError: Input/Output exception.\n");
            }
        }
        System.out.println();
    }

    private void getTableIntoStringBuffer() {
        try {
            while(!(dataString = dataReader.readLine()).matches(
                    ".*<hr\\s*size=\"2\"\\s*color=\"#784b04\">.*")) {
                this.businessInfoBuffer.append(dataString);
            }
        } catch (IOException ioe) {
            System.out.println("\nError: Input/Output exception.\n");
        }
    }

    private boolean createDBO(DBObject dbo) {
        boolean orgNameAssigned = this.parseOrganizationData(dbo);
        if (!orgNameAssigned) {
            return false;
        }
        this.parseWebAddress(dbo);
        boolean gotAddress = this.parseAddress(dbo);
        if (!gotAddress) {
            return false;
        } 
        this.setCountryIfEmpty(dbo);
        this.getZipCode(dbo);
        this.getTelephone(dbo);
        this.getSpecialty(dbo);
        this.addCityToHash(dbo);
        this.checkIfCityInUAE(dbo);
        this.getIndustries(dbo);
        return true;
    }

    private boolean parseOrganizationData(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] parsedOrganizationArray = 
                businessInfoString.split("Company\\s*?Name:");
        boolean organizationNameWasSet = 
                this.setOrganizationName(dbo, parsedOrganizationArray);
        if (organizationNameWasSet) {
            return true;
        } else {
            return false;
        }
    }

    private boolean setOrganizationName(DBObject dbo, 
            String[] parsedOrganizationArray) {
        if (parsedOrganizationArray.length > 1) {
            parsedOrganizationArray = parsedOrganizationArray[1].split("</b>");
            if (parsedOrganizationArray.length > 1) {
                String parsedOrganizationName = parsedOrganizationArray[0];
                if (parsedOrganizationName.length() > 200) {
                    parsedOrganizationName = 
                            parsedOrganizationName.substring(0, 200);
                }
                parsedOrganizationName = 
                        this.removeNonWordCharactersFromBeginningAndEnd
                        (parsedOrganizationName);
                dbo.setOrganizationName(parsedOrganizationName);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void getEmail(DBObject dbo) {
        String businessInfoString =  this.businessInfoBuffer.toString();
        String[] parsedEmailArray = 
                businessInfoString.split("Email Address:");
        parsedEmailArray = parsedEmailArray[1].split(">");
        parsedEmailArray = parsedEmailArray[1].split("</a");
        String parsedEmail = parsedEmailArray[0];
        dbo.setEmailAddress(parsedEmail);
    }
    
    private void parseWebAddress(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        Pattern webAddressPatt =  Pattern.compile("(.*)(Web[\\s]*Address:)" +
                 "([^<]*)(.*)");
        Matcher webAddressMatch = webAddressPatt.matcher(businessInfoString);
        if (webAddressMatch.matches()) {
            String webAddress = webAddressMatch.group(3);
            dbo.setWebAddress(webAddress);
        }
    }

    private boolean parseAddress(DBObject dbo) {
        this.getCity(dbo);
        boolean gotStreetAddress = this.getStreetAddress(dbo);
        if (!gotStreetAddress) {
            return false;
        }
        this.getEmail(dbo);
        return true;
    }

    private boolean addDBOToVector(DBObject dbo) {
        if (this.getDBOVector().size() < 15000) {
            this.getDBOVector().add(dbo);
            return true;
        } else {
            return false;
        }
    }

    private void getCity(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedCityNameArray = businessInfoString.split("City:");
        if (unparsedCityNameArray.length > 1) {
            String[] parsedCityNameArray = unparsedCityNameArray[1].split("</td>");
            if (parsedCityNameArray.length > 1) {
                String parsedCityName = parsedCityNameArray[0];
                if (parsedCityName.length() > 200) {
                    parsedCityName = parsedCityName.substring(0, 200);
                }
                parsedCityName = this.removeNonWordCharactersFromBeginningAndEnd
                        (parsedCityName);
                dbo.setCity(this.formatCityHashKey(parsedCityName));
            }
        } 
        String[] unparsedRegionArray = businessInfoString.split("Main region covered:");
        if (unparsedRegionArray.length > 1) {
            String[] parsedRegionArray = unparsedRegionArray[1].split("</td>");
            if (parsedRegionArray.length > 1) {
                String parsedRegionString = parsedRegionArray[0];
                if (parsedRegionString.length() > 200) {
                    parsedRegionString = parsedRegionString.substring(0, 200);
                }
                if (dbo.getCity() == null) {
                    parsedRegionString = 
                            this.removeNonWordCharactersFromBeginningAndEnd
                            (parsedRegionString);
                    dbo.setCity(this.formatCityHashKey(parsedRegionString));
                }
            }
        }
    }

    private boolean getStreetAddress(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedAddressArray = businessInfoString.split(">Address:");
        if (unparsedAddressArray.length > 1) {
           String[] parsedAddressArray = 
                   unparsedAddressArray[1].split("</td>"); 
           if (parsedAddressArray.length > 1) {
               String parsedAddress = parsedAddressArray[0];
               if (parsedAddress.length() > 200) {
                   parsedAddress = parsedAddress.substring(0, 200);
               }
               parsedAddress = this.searchStateAndCountry(parsedAddress, dbo);
               parsedAddress = this.searchCityRecurrence(parsedAddress, dbo);
               parsedAddress = this.searchUnit(parsedAddress, dbo);
               parsedAddress = this.searchPhoneAndFax(parsedAddress, dbo);
               parsedAddress = 
                       this.removeNonWordCharactersFromBeginningAndEnd
                       (parsedAddress);
               boolean isStreetAddress = 
                       this.checkForEmptyStreetAddress(parsedAddress, dbo);
               parsedAddress = this.replaceNonWordCharactersInAddressWithSpace(
                       parsedAddress);
               if (!isStreetAddress) {
                   return false;
               }
               dbo.setStreetName(parsedAddress);
           }
        }
        return true;
    }
    
    private String searchStateAndCountry(String parsedAddress, DBObject dbo) {
        parsedAddress =  this.pullOutStateAndCountry(parsedAddress, dbo);
        return parsedAddress;
    }
    
    private String pullOutStateAndCountry(String parsedAddress, DBObject dbo) {
        Pattern dubaiPatt = Pattern.compile("(.*)([dD][uU][bB][aA][iI][,-]?)(.*)");
        Matcher stateMatch = dubaiPatt.matcher(parsedAddress);
        //Pattern abuDhabiPatt = Pattern.compile("(.*)")
        if (stateMatch.matches()) {
            String state = stateMatch.group(2);
            state = this.removeNonWordCharactersFromBeginningAndEnd(state);
            dbo.setState(state);
            parsedAddress = stateMatch.group(1) 
                    + stateMatch.group(3);
            parsedAddress = this.pullOutCountry(parsedAddress, dbo);
            parsedAddress =  this.removeNonWordCharactersFromBeginningAndEnd(
                    parsedAddress);
        }
        return parsedAddress;
    }
    private String pullOutCountry(String parsedAddress, DBObject dbo) {
        Pattern countryPatt = Pattern.compile("(.*)([uU][\\.]?[\\s]*?[aA]" + 
                 "[\\.]?[\\s]*?[eE][\\.]?[\\s]*?)(.*)");
        Matcher countryMatch = countryPatt.matcher(parsedAddress);
        if (countryMatch.matches()) {
            String country = countryMatch.group(2);
            country = this.removeNonWordCharactersFromBeginningAndEnd(country);
            dbo.setCountry(country);
            parsedAddress = countryMatch.group(1) + countryMatch.group(3);
            parsedAddress = this.removeNonWordCharactersFromBeginningAndEnd(
                    parsedAddress);
            return  parsedAddress;
        }
        return parsedAddress;
    }
    
    private String searchCityRecurrence(String address, DBObject dbo) {
        String city = dbo.getCity();
        try {
        if (address.matches(".*" + city + ".*")) {
            address = address.replaceAll(city + "[\\W]?", "");
        }
        } catch (java.util.regex.PatternSyntaxException pse) {
            System.out.println();
        }
        return address;
    }
    
    private String searchUnit(String address, DBObject dbo) {
        address = this.searchPOBox(address, dbo);
        address = this.searchSuiteOrOffice(address, dbo);
        return address;
    }
    
    private String searchPhoneAndFax(String address, DBObject dbo) {
        Pattern phoneNumberInAddressPatt = Pattern.compile("(.*)" + 
                "([tT][eE][lL][\\s][nN]?[oO]?[:]?[\\s]*)([\\d-]*)(.*)");
        Matcher phoneNumberInAddressMatch = 
                phoneNumberInAddressPatt.matcher(address);
        if (phoneNumberInAddressMatch.matches()) {
            address = phoneNumberInAddressMatch.group(1) +
                    phoneNumberInAddressMatch.group(4);
            return address;
        }
        return address;
    }
    
    private String searchPOBox(String address, DBObject dbo) {
        Pattern POPatt = Pattern.compile("(.*)([pP][\\.]?[\\s]*[oO][\\.]?[\\s]" + 
                "*[sS]?[tT]?[\\s]*[bB][oO][xX][:]?[\\s]*[\\d]*)(.*)");
        Matcher m = POPatt.matcher(address);
        if (m.matches()) {
            String unit = m.group(2);
            unit = this.removeNonWordCharactersFromBeginningAndEnd(unit);
            dbo.setUnit(unit);
            address = m.group(1) + m.group(3);
            address = this.removeNonWordCharactersFromBeginningAndEnd(
                    address);
            return address;
        } else {
            return address;
        }
    }
    
    private String searchSuiteOrOffice(String address, DBObject dbo) {
        Pattern suitePatt = Pattern.compile("(.*)([sS][uU][iI][tT][eE]"
                + "[\\s]*[nN]?[oO]?[\\.]?[\\s]*[\\d]*)(.*)");
        Matcher suiteMatch = suitePatt.matcher(address);
        if (suiteMatch.matches()) {
            String suite = suiteMatch.group(2);
            suite = this.removeNonWordCharactersFromBeginningAndEnd(suite);
            dbo.setUnit(suite);
            address = suiteMatch.group(1) + suiteMatch.group(3);
        }
        
        Pattern officePatt = Pattern.compile("(.*)([oO][fF][fF][iI][cC][eE][\\s]*[#]?" 
                + "[\\s]*[nN]?[oO]?[\\.]?[\\s]*[\\d][\\s]*[\\w]?)(.*)");
        Matcher officeMatch = officePatt.matcher(address);
        if (officeMatch.matches()) {
            String office = officeMatch.group(2);
            String otherStuff = officeMatch.group(3);
            office = this.removeNonWordCharactersFromBeginningAndEnd(office);
            dbo.appendUnit(office);
            address = officeMatch.group(1) + officeMatch.group(3);
        }
        return address;
    }
    
    private boolean checkForEmptyStreetAddress(String parsedAddress, 
            DBObject dbo) {
        if (parsedAddress.matches("^[\\s]*$")) {
            if (dbo.getWebAddress() == null) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }
    
    private String removeNonWordCharactersFromBeginningAndEnd(String 
            data) {
        Pattern nonWordCharPatt = Pattern.compile("(^[\\W]*)([\\w].*[\\w])?([\\W]*)");
        Pattern onlyNonWordCharPatt = Pattern.compile("^[\\W]*$");
        Matcher nonWordCharMatcher = nonWordCharPatt.matcher(data);
        Matcher onlyNonWordCharMatcher = onlyNonWordCharPatt.matcher(data);
        if (onlyNonWordCharMatcher.matches()) {
            data = "";
        } else if (nonWordCharMatcher.matches()) {
            data = data.replaceAll("[\\s]+", " ");
            data = nonWordCharMatcher.group(2);
        }
        data = data.trim();
        data = data.replaceAll("[\\s]+", " ");
        return data;
    }

    private void getTelephone(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        String[] unparsedTelephoneArray = businessInfoString.split("Tel:"); 
        if (unparsedTelephoneArray.length > 1) {
            String[] parsedTelephoneArray = 
                    unparsedTelephoneArray[1].split("</td>");
            if (parsedTelephoneArray.length > 1) {
                String parsedTelephone = parsedTelephoneArray[0];
                if (parsedTelephone.length() > 200) {
                    parsedTelephone = parsedTelephone.substring(0, 200);
                }
                parsedTelephone = this.formatPhone(parsedTelephone);
                dbo.setPhone1(parsedTelephone);
            }
        }
    }
    
    private String formatPhone(String phone) {
        phone = phone.replaceAll("[^\\d]", "").trim();
        Pattern phoneNumber = Pattern.compile("([\\d]*)([\\d]{7})");
        Matcher phoneNumberMatcher = phoneNumber.matcher(phone);
        if (phoneNumberMatcher.matches()) {
            phone = phoneNumberMatcher.group(1) + "-" +
                    phoneNumberMatcher.group(2).substring(0,3) + "-" + 
                    phoneNumberMatcher.group(2).substring(3,7);
            phone = this.removeNonWordCharactersFromBeginningAndEnd(phone);
        } 
        Pattern countryCodePatt = 
                Pattern.compile("([\\d-]*)([9][7][1])([\\d-]*)");
        Matcher countryCodeMatch = countryCodePatt.matcher(phone);
        if (countryCodeMatch.matches()) {
            phone = countryCodeMatch.group(1) + "-" + countryCodeMatch.group(2)
                    + "-" + countryCodeMatch.group(3);
            phone = this.removeNonWordCharactersFromBeginningAndEnd(phone);
        }
        return phone;
    }
    
    private String replaceNonWordCharactersInAddressWithSpace(
            String address) {
        address = address.replaceAll("[\\W]", " ");
        return address;
    }
    
    private void setCountryIfEmpty(DBObject dbo) {
        if (dbo.getCountry() == null) {
            String businessInfoString = this.businessInfoBuffer.toString();
            Pattern countryPatt = Pattern.compile("(.*)([uU][\\.]?[\\s]*[aA]" +
                    "[\\.]?[\\s]*[eE][\\.]?[\\s])(.*)|(.*)([dD][uU][bB][aA][iI]" +
                    ")(.*)");
            Matcher countryMatch = countryPatt.matcher(businessInfoString);
            if (countryMatch.matches()) {
                dbo.setCountry("UAE");
            }
        }
    }
    
    private void getSpecialty(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        Pattern specialtyPatt = Pattern.compile("(.*)(Description of services:)" +
                "([^<]*)(.*)");
        Matcher specialtyMatcher = specialtyPatt.matcher(businessInfoString);
        if (specialtyMatcher.matches()) {
            String specialty =  specialtyMatcher.group(3);
            if (specialty.length() > 200) {
                specialty = specialty.substring(0, 200);
            }
            dbo.setSpecialty(specialty);
        }
    }
    
    private void addCityToHash(DBObject dbo) {
        if (((dbo.getCountry() != null) 
                && (dbo.getCountry().matches("[uU][aA][eE]")))
                || ((dbo.getState() != null) &&
                (dbo.getState().matches("[dD][uU][bB][aA][iI]")))) {
            if (dbo.getCity() != null) {
                this.cities.put(dbo.getCity(), "UAE");            } 
        }
    }
    
    private void checkIfCityInUAE(DBObject dbo) {
        if (dbo.getCountry() == null) {
            if (this.cities.containsKey(dbo.getCity())) {
                dbo.setCountry("UAE");
            }
        }
    }
    
    private String formatCityHashKey(String city) {
        city = city.replaceAll("[\\s\\W]+", " ");
        city = city.toUpperCase();
        return city;
    }
    
    private void getIndustries(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        Pattern industryPatt = Pattern.compile("(.*)(Services offered :)"
                + "([^<]*)(.*)");
        Matcher industryMatch = industryPatt.matcher(businessInfoString);
        if (industryMatch.matches()) {
            String unparsedIndustries = industryMatch.group(3);
            String[] parsedIndustries = unparsedIndustries.split(",");
            for (String industry : parsedIndustries) {
                Industry ind = new Industry();
                ind.setName(industry.toUpperCase().trim());
                dbo.getIndustries().add(ind);
            }
        }
    }
    
    private void getZipCode(DBObject dbo) {
        String businessInfoString = this.businessInfoBuffer.toString();
        Pattern zipCodePatt = Pattern.compile("(.*)(Post Code:)([^<]*)(.*)");
        Matcher zipCodeMatch =  zipCodePatt.matcher(businessInfoString);
        if (zipCodeMatch.matches()) {
            String zip = zipCodeMatch.group(3);
            zip = zip.replaceAll("[^\\d]", "");
            zip = zip.trim();
            dbo.setZipCode(zip);
        }
    }
}		    