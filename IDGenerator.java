package parsedata;

import java.util.Hashtable;
import java.util.Vector;
import com.princeton.database.global.entity.Keyword;
import com.princeton.database.global.entity.KeywordPK;
import com.princeton.database.directory.entity.Industry;
import com.princeton.database.directory.entity.IndustryPK;
import com.princeton.database.directory.entity.Organization;
import com.princeton.database.directory.entity.OrganizationPK;
import com.princeton.database.directory.entity.AddressPK;
import com.princeton.database.directory.entity.OrganizationKeyword;
import com.princeton.database.directory.entity.OrganizationKeywordPK;
import com.princeton.database.directory.entity.Category;
import com.princeton.database.directory.entity.CategoryPK;
import com.princeton.database.directory.session.DirectoryDatabaseFacade;
import java.util.List;
import java.util.ListIterator;
import java.util.Enumeration;

public class IDGenerator {
    
    private Hashtable keywordsList, industryList, orgList, orgKeyList, iccList;
    private Vector<DBObject> dboVector;
    private int keywordSeed, industrySeed, keywordOffset, industryOffset, 
            nullsSkipped, doubles, notLegitNames, DBObjects, orgIdPrefix;
    private DirectoryDatabaseFacade sessionBean;
    private String icc;

    public IDGenerator(DirectoryDatabaseFacade session, String icc) {
        this.icc = icc;
        this.iccList = new Hashtable();
        this.populateIccList();
        this.orgIdPrefix = (int) this.iccList.get(icc);
        this.sessionBean = session;
        this.keywordsList = new Hashtable();
        this.loadDatabaseKeywords();
        this.industryList = new Hashtable();
        this.loadDatabaseIndustries();
        this.orgList = new Hashtable();
        this.orgKeyList = new Hashtable();
        this.loadDatabaseOrganizations();
        this.keywordSeed = 1 + keywordsList.size();
        this.industrySeed = 1 + industryList.size();
        this.nullsSkipped = 0;
        this.DBObjects = 0;
    }
    
    public DirectoryDatabaseFacade getSessionBean() {
        return this.sessionBean;
    }
    
    // This  should grow with the number of countries mined 
    private void populateIccList() {
        this.iccList.put("AE", 971);
    }
    
    public void loadDatabaseKeywords() {
        List keywords = this.getSessionBean().getAllKeywords();
        ListIterator listIter = keywords.listIterator();
        while (listIter.hasNext()) {
            Keyword key = (Keyword) listIter.next();
            KeywordPK keyPK = key.getKeywordPK();
            String id = keyPK.getId();
            String keyword = key.getKeyword();
            this.keywordsList.put(keyword, id);
        }
    }
    
    public void loadDatabaseIndustries() {
        List industries = this.getSessionBean().getAllIndustries();
        ListIterator listIter = industries.listIterator();
        while (listIter.hasNext()) {
            Industry ind = (Industry) listIter.next();
            IndustryPK indPK = ind.getIndustryPK();
            String id = indPK.getId();
            String industry = ind.getName();
            this.industryList.put(industry, id);
        }
    }
    
    public void loadDatabaseOrganizations() {
        List organizations =  this.getSessionBean().getAllOrganizations();
        ListIterator listIter = organizations.listIterator();
        while (listIter.hasNext()) {
            Organization org = (Organization) listIter.next();
            OrganizationPK orgPK = org.getOrganizationPK();
            String orgId = orgPK.getId();
            String orgName = org.getName();
            this.orgList.put(orgName, orgId);
            this.orgKeyList.put(orgId, orgName);
        }
    }
    
    public void passDBOVector(Vector<DBObject> dboVec) {
        this.dboVector = dboVec;
        this.DBObjects += dboVec.size();
    }
    
    public int getDoubles() {
        return this.doubles;
    }
    
    private Hashtable getKeywordsList() {
        return this.keywordsList;
    }
    
    private Hashtable getIndustryList() {
        return this.industryList;
    }
    
    public int getNotLegitNames() {
        return this.notLegitNames;
    }
    
    private int getNullsSkipped() {
        return this.nullsSkipped;
    }
    
    private Hashtable getOrgList() {
        return this.orgList;
    }
    
    private Hashtable getOrgKeyList() {
        return this.orgKeyList;
    }
    
    private int getIndustrySeed() {
        return this.industrySeed;
    }
    
    private int getKeywordSeed() {
        return this.keywordSeed;
    }
    
    private void incrementDoubles() {
        this.doubles++;
    }
    
    private void incrementKeywordSeed() {
        this.keywordSeed++;
    }
    
    private void incrementNotLegitNames() {
        this.notLegitNames++;
    }
    
    private int incrementIndustrySeed() {
        return this.industrySeed++;
    }

    public Vector<DBObject> getDBOVector() {
        return this.dboVector;
    }
    
    private void incrementNullsSkipped() {
        this.nullsSkipped++;
    }
    
    public void generateIDs() {
        this.newKeyword();
        this.newIndustry();
        this.newOrganization();
        this.newOrganizationKeywordEntity();
        this.newCategoryEntity();
        System.out.println("Number of database objects: " + 
                DBObjects);
        System.out.println("Number of unique categories in hash: " +
                industryList.size());
        System.out.println("Number of unique keywords in hash: " +
                keywordsList.size());
        System.out.println("Number of unique organization names in hash: "+
                orgList.size());
        System.out.println("Number of unique organization keys in hash: " +
                orgKeyList.size());
        System.out.println(nullsSkipped);
    }

    private void newKeyword() {
        Vector<DBObject> dboVector = this.getDBOVector();
        for (int i = 0; i < dboVector.size(); i++) {
            DBObject dbo = dboVector.get(i);
            this.getKeyword(dbo);
        }
    }

    private void getKeyword(DBObject dbo) {
        for (int i = 0; i < dbo.getKeywords().size(); i++) {
            String keyword = dbo.getKeywords().get(i).getKeyword();
            if (!this.getKeywordsList().containsKey(keyword)) {
                int id = this.getKeywordSeed();
                this.incrementKeywordSeed();
                this.getKeywordsList().put(keyword, new Integer(id));
                KeywordPK newPK = new KeywordPK
                        (String.valueOf(id), "BIZ");
                dbo.getKeywords().get(i).setKeywordPK(newPK);
            } else {
                String keyID = this.getExistingKeyword(keyword, 
                        this.getKeywordsList());
                KeywordPK newPK = new KeywordPK
                        (keyID, "BIZ");
                dbo.getKeywords().get(i).setKeywordPK(newPK);
            }
        }
    }
    
    private String getExistingKeyword(String keyword, 
            Hashtable keywordsList) {
        String keyID = keywordsList.get(keyword).toString();
        return keyID;
    }
    
    private void newIndustry() {
        Vector<DBObject> dboVector = this.getDBOVector();
        for (int i = 0; i < dboVector.size(); i++) {
            DBObject dbo = dboVector.get(i);
            this.getIndustry(dbo);
        }
    }
    
    private void getIndustry(DBObject dbo) {
        for (int i = 0; i < dbo.getIndustries().size(); i++) {
            String name = dbo.getIndustries().get(i).getName().toUpperCase();
            if (!this.getIndustryList().containsKey(name)) {
                String id = Integer.toString(this.getIndustrySeed());
                id = this.iccList.get(this.icc) + id;
                this.incrementIndustrySeed();
                this.getIndustryList().put(name.toUpperCase(),  new Integer(id));
                IndustryPK newPK = new IndustryPK
                        ("AE", String.valueOf(id), "BIZ");
                dbo.getIndustries().get(i).setIndustryPK(newPK);
            } else {
                String indID = 
                        this.getExistingIndustry(name, this.getIndustryList());
                IndustryPK newPK = new IndustryPK
                        ("AE", indID, "BIZ");
                dbo.getIndustries().get(i).setIndustryPK(newPK);
            }
        }
    }

    private String getExistingIndustry(String name, 
            Hashtable industryList) {
        String indID;
        indID = industryList.get(name).toString();
        return indID;
    }
    
    private void newOrganization() {
        Vector<DBObject> dboVector = this.getDBOVector();
        for (int i = 0; i < dboVector.size(); i++) {
            DBObject dbo = dboVector.get(i);
            boolean orgWasSet = this.getOrganization(dbo);
            if (!orgWasSet)  {
                i--;
            }
        }
    }
    
    private boolean getOrganization(DBObject dbo) {
        String orgName = dbo.getOrganizationName().replaceAll("[\\s]+", " ");
        orgName = orgName.replaceAll("[^\\w\\d\\s]*", "");
        orgName = orgName.toUpperCase();
        dbo.setOrganizationName(orgName);
        if (!this.isLegitOrgName(orgName)) {
            this.getDBOVector().remove(dbo);
            
            // Debugging
            this.incrementNotLegitNames();
            return false;
        }

        if (!this.getOrgList().containsKey(orgName)) {
            this.getOrgAndAddrPk(dbo, orgName);
            return true;
        } else {
            this.getDBOVector().remove(dbo);
            
            // Debugging
            this.incrementDoubles();
            return false;
        }
    }
    
    private void getOrgAndAddrPk(DBObject dbo, String orgName) {
        String id = this.generateID(orgName, this.getOrgKeyList());
        this.addHashKeys(orgName, id, this.getOrgList(), this.getOrgKeyList());
        OrganizationPK newOrgPK = new OrganizationPK( id,  "AE",  "BIZ");
        dbo.getOrganization().setOrganizationPK(newOrgPK);
        AddressPK newAddrPK = new AddressPK(id, "AE", "BIZ");
        dbo.getAddress().setAddressPK(newAddrPK);
    }

    private void newOrganizationKeywordEntity() {
        Vector<DBObject> dboVector = this.getDBOVector();
        for (int i = 0; i < dboVector.size(); i++) {
            DBObject dbo = dboVector.get(i);
            this.getOrgPK(dbo);
        }
    }
    
    private void getOrgPK(DBObject dbo) {
        Organization org = dbo.getOrganization();
        OrganizationPK orgPK = org.getOrganizationPK();
        for (int i = 0; i < dbo.getKeywords().size(); i++) {
            Keyword  key = dbo.getKeywords().get(i);
            KeywordPK keyPK = key.getKeywordPK();
            OrganizationKeywordPK orgKeyPK = 
                    new OrganizationKeywordPK(keyPK.getId(), orgPK.getId(),
                    orgPK.getIcc());
            OrganizationKeyword orgKey = 
                    new OrganizationKeyword(orgKeyPK);
            dbo.getOrganizationKeywords().add(orgKey);
        }
    }
    
    private void newCategoryEntity() {
        Vector<DBObject> dboVector = this.getDBOVector();
        for (int i = 0; i < dboVector.size(); i++) {
            DBObject dbo = dboVector.get(i);
            this.getNewCategoryEntity(dbo);
        }
    }
    
    private void getNewCategoryEntity(DBObject dbo) {
        Organization org = dbo.getOrganization();
        OrganizationPK orgPK = org.getOrganizationPK();
        for (int i = 0; i < dbo.getIndustries().size(); i++) {
            if (orgPK == null) {
                System.out.println("\n\n\nnull skipped\n\n\n");
                this.incrementNullsSkipped();
                continue;
            }
            Industry ind = dbo.getIndustries().get(i);
            IndustryPK indPK = ind.getIndustryPK();
            CategoryPK catPK = new CategoryPK(ind.getIndustryPK().getIcc(), 
                    org.getOrganizationPK().getIcc(), orgPK.getId(), 
                    indPK.getId());
            Category cat = new Category(catPK);
            dbo.getCategories().add(cat);
            if ((catPK.getOrganizationId().equals("9712626683")) &&
                    (catPK.getIndustryId().equals("971503"))) {
                
                System.out.println();
            }
        }
    }
    
    private boolean isLegitOrgName(String orgName) {
        if (orgName.matches(".*[a-zA-Z].*")) { 
            return true;
        } else {
            return false;
        }
    }
    
    private String generateID(String orgName, Hashtable orgKeyList) {
        // This is the object for generating organization IDs according to
        // phone keys
        PhoneKeyIdGen idGen = new PhoneKeyIdGen(7);
        String id = idGen.generateId(orgName); 
        id = this.orgIdPrefix + id;

        // Now we have to check if the id already exists, and if so, 
        //  increment ours to  make it unique
        Integer appendix = new Integer(0);
        if (orgKeyList.containsKey(id)) {
            while (orgKeyList.containsKey(id + appendix.toString())) {
                appendix++;
            }
            id += appendix.toString();
        }
        return id;
    }
    
    public void addHashKeys(String orgName, String id, Hashtable orgList,
            Hashtable orgKeyList) {
        orgList.put(orgName, id);
        orgKeyList.put(id, orgName);
    }
}
