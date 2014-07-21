package org.nustaq.machnetz.model;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.ColOrder;
import org.nustaq.reallive.sys.annotations.Description;

/**
 * Created by ruedi on 11.07.14.
 */
@Description("Person Records")
public class TestRecord extends Record {

    @ColOrder(0) String name;
    @ColOrder(10) String preName;
    @ColOrder(20) int yearOfBirth;
    @ColOrder(17) String sex;
    @ColOrder(15) String profession;

    public TestRecord() {
    }

    public TestRecord(String name, String preName, int yearOfBirth, String sex, String profession) {
        this.name = name;
        this.preName = preName;
        this.yearOfBirth = yearOfBirth;
        this.sex = sex;
        this.profession = profession;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPreName() {
        return preName;
    }

    public void setPreName(String preName) {
        this.preName = preName;
    }

    public int getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(int yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

}
