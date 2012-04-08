package parsedata;

import java.util.Vector;
import com.princeton.database.directory.entity.Address;
import com.princeton.database.directory.entity.AddressPK;
import com.princeton.database.directory.entity.Organization;
import com.princeton.database.directory.entity.OrganizationPK;
import com.princeton.database.global.entity.Keyword;
import com.princeton.database.global.entity.KeywordPK;
import com.princeton.database.directory.entity.Industry;
import com.princeton.database.directory.entity.IndustryPK;
import com.princeton.database.directory.entity.Category;
import com.princeton.database.directory.entity.CategoryPK;
import com.princeton.database.directory.entity.OrganizationKeyword;
import com.princeton.database.directory.entity.OrganizationKeywordPK;
import com.princeton.database.directory.session.DirectoryDatabaseFacade;

public class Insert {
    
    private DirectoryDatabaseFacade sessionBean;
    
    // Debugging
    private static int nullOrganizationCount = 0;
    
    public Insert() {
        initSessionBean();
    }
    
    private void initSessionBean() {
        sessionBean = new DirectoryDatabaseFacade();
        sessionBean.init("CLASSIFIEDS_PU");
    }
    
    public DirectoryDatabaseFacade getSessionBean() {
        return this.sessionBean;
    }
    
    public void insertIntoDatabase(Vector<DBObject> dboVector) {
         for (int i = 0; i < dboVector.size(); i++) {
             insertDBOIntoDatabase(dboVector.get(i));
         }
         System.out.println("Null Organization Count: " + nullOrganizationCount);
    }
    
    private void insertDBOIntoDatabase(DBObject dbo) {
        Organization org = dbo.getOrganization();
        insertOrganization(org);
        
        Address addr = dbo.getAddress();
        insertAddress(addr);
        
        Vector<Keyword> keywords = dbo.getKeywords();
        insertKeywords(keywords);
        
        Vector<Industry> industries = dbo.getIndustries();
        insertIndustries(industries);
        
        Vector<Category> categories = dbo.getCategories();
        insertCategories(categories);
        
        Vector<OrganizationKeyword> organizationKeywords = 
                dbo.getOrganizationKeywords();
        insertOrganizationKeyword(organizationKeywords);
    }
    
    private void insertOrganization(Organization org) {
        try {
            sessionBean.persistOrganization(org);
        } catch (NullPointerException e) {
            
            // Debugging
            nullOrganizationCount++;
            System.out.println();
        }
    }
    
    private void insertAddress(Address addr) {
        sessionBean.persistAddress(addr);
    }
    
    private void insertKeywords(Vector<Keyword> keys) {
        for (int i = 0; i < keys.size(); i++) {
            try {
                Keyword key = keys.get(i);
                sessionBean.persistKeyword(key);
            } catch (IllegalStateException e) {
                continue;
            }
        }
    }
    
    // This used to be Categories
    private void insertIndustries(Vector<Industry> industries) {
        for (int i = 0; i < industries.size(); i++) {
            try {
                Industry ind = industries.get(i);
                sessionBean.persistIndustry(ind);
            } catch (IllegalStateException e) {
                continue;
            }
        }
    }
    
    private void insertCategories(Vector<Category> categories) {
        for (int i = 0; i < categories.size(); i++) {
            try {
                Category cat = categories.get(i);
                System.out.println();
                cat.setWeight(1);
                sessionBean.persistCategory(cat);
            } catch (Exception e) {
                System.out.println();
            }
        }
    }
    
    private void insertOrganizationKeyword
            (Vector<OrganizationKeyword> organizationKeywords) {
        for (int i = 0; i < organizationKeywords.size(); i++) {
            OrganizationKeyword orgKey = organizationKeywords.get(i);
            orgKey.setDatePurchased("");
            orgKey.setDateExpires("");
            sessionBean.persistOrganizationKeyword(orgKey);
        }
    }
}

