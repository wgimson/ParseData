package parsedata;

import com.princeton.database.directory.entity.Address;
import com.princeton.database.directory.entity.Organization;
import com.princeton.database.directory.entity.Industry;
import com.princeton.database.directory.entity.Category;
import com.princeton.database.global.entity.Keyword;
import com.princeton.database.directory.entity.OrganizationKeyword;
import java.util.Vector;

public class DBObject {
    
    Organization org;
    Address addr;
    Vector<Keyword> keywords;
    Vector<Industry> industries; 
    Vector<Category> categories;
    Vector<OrganizationKeyword> organizationKeywords;
    String country;
    
    public DBObject() {
        this.org = new Organization();
        this.addr = new Address();
        this.keywords = new Vector<Keyword>();
        this.industries = new Vector<Industry>();
        this.categories = new Vector<Category>();
        this.organizationKeywords = new Vector<OrganizationKeyword>();
    }
    
    // Getters
    public String getCrossStreet() {
        return this.addr.getCrossStreet();
    }
    
    public Organization getOrganization() {
        return this.org;
    }
    
    public String getEmailAddress() {
        return this.addr.getEmailAddress();
    }
    
    public Vector<OrganizationKeyword> getOrganizationKeywords() {
        return this.organizationKeywords;
    }
    
    public String getOrganizationName() {
        return this.org.getName();
    }
    
    public String getPhone1() {
        return this.addr.getPhone1();
    }
    
    public String getPhone1Type() {
        return this.addr.getPhone1Type();
    }
    
    public String getPhone2() {
        return this.addr.getPhone2();
    }
    
    public String getPhone2Type() {
        return this.addr.getPhone2Type();
    }
    
    public String getFax() {
        return this.addr.getFax();
    }
    
    public Address getAddress() {
        return this.addr;
    }
    
    public Vector<Category> getCategories() {
        return this.categories;
    }
    
    public Vector<Keyword> getKeywords() {
        return this.keywords;
    }
    
    public Vector<Industry> getIndustries() {
        return  this.industries;
    }
    
    public String getCity() {
        return this.addr.getCity();
    }
    
    public String getSpecialty() {
        return this.org.getSpecialty();
    }
    
    public String getStreetName() {
        return this.addr.getStreetName();
    }
    
    public String getUnit() {
        return this.addr.getUnit();
    }
    
    public String getState() {
        return this.addr.getState();
    }
    
    public String getCountry() {
        return this.country;
    }
    
    public String getWebAddress() {
        return this.addr.getUrl();
    }
    
    public String getZipCode() {
        return this.addr.getZipCode();
    }
    
    // Setters
    public void setCrossStreet(String street) {
        this.addr.setCrossStreet(street);
    }
    
    public void setEmailAddress(String email) {
        this.addr.setEmailAddress(email);
    }
    
    public void setFax(String fax) {
        this.addr.setFax(fax);
    }
    
    public void setOrganization(Organization org) {
        this.org = org;
    }
    
    public void setOrganizationName(String name) {
        this.org.setName(name);
    }
    
    public void setOrganizationKeywords(Vector<OrganizationKeyword> orgKeys) {
        this.organizationKeywords = orgKeys;
    }
    
    public void setPhone1(String phone) {
        this.addr.setPhone1(phone);
        this.addr.setPhone1Type("PRIMARY");
    }
    
    public void setPhone1Type(String type) {
        this.addr.setPhone1Type(type);
    }
    
    public void setPhone2(String phone) {
        this.addr.setPhone2(phone);
    }
    
    public void setPhone2Type(String type) {
        this.addr.setPhone2Type(type);
    }
    
    public void setAddress(Address addr) {
        this.addr = addr;
    }
    
    public void setCategories(Vector<Category> cats) {
        this.categories = cats;
    }
    
    public void setKeywords(Vector<Keyword> keys) {
        this.keywords = keys;
    }
    
    public void setIndustries(Vector<Industry> inds) {
        this.industries = inds;
    }
    
    public void setCity(String city) {
        this.addr.setCity(city);
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public void setOrgName(String name) {
        this.org.setName(name);
    }
    
    public void setSpecialty(String spec) {
        this.org.setSpecialty(spec);
    }
    
    public void setStreetName(String street) {
        this.addr.setStreetName(street);
    }
    
    public void setUnit(String unit) {
        this.addr.setUnit(unit);
    }
    
    public void appendUnit(String appUnit) {
        if (this.getUnit() != null) {
            this.addr.setUnit(this.getUnit() + appUnit);
        } else {
            this.addr.setUnit(appUnit);
        }
    }
    
    public void setState(String state) {
        this.addr.setState(state);
    }
    
    public void setWebAddress(String webAddress) {
        this.addr.setUrl(webAddress);
    }
    
    public void setZipCode(String zip) {
        this.addr.setZipCode(zip);
    }
}

