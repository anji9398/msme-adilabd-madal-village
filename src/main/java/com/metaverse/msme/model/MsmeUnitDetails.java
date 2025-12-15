package com.metaverse.msme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity
@Table(name = "msme_unit_details")
public class MsmeUnitDetails {

    @Id
    @Column(name = "slno")
    private Integer slno;

    @Column(name = "uniqueno")
    private String uniqueNo;

    @Column(name = "departmentname")
    private String departmentName;

    @Column(name = "msmestate")
    private String msmeState;

    @Column(name = "msmesdist")
    private String msmeDist;

    @Column(name = "msmessector")
    private String msmeSector;

    @Column(name = "unitname")
    private String unitName;

    @Column(name = "category")
    private String category;

    @Column(name = "unitaddress")
    private String unitAddress;

    @Column(name = "doorno")
    private String doorNo;

    @Column(name = "locality")
    private String locality;

    @Column(name = "street")
    private String street;

    @Column(name = "villageid")
    private String villageid;

    @Column(name = "village")
    private String village;

    @Column(name = "ward")
    private String ward;

    @Column(name = "mandal")
    private String mandal;

    @Column(name = "district")
    private String district;

    @Column(name = "pincode")
    private String pinCode;

    @Column(name = "officeemail")
    private String officeEmail;

    @Column(name = "officecontact")
    private String officeContact;

    @Column(name = "principalbusinessplace")
    private String principalBusinessPlace;

    @Column(name = "femaleempstotal")
    private String femaleEmpsTotal;

    @Column(name = "maleempstotal")
    private String maleEmpsTotal;

    @Column(name = "lattitude")
    private String latitude;

    @Column(name = "longitute")
    private String longitude;

    @Column(name = "institutiondetails")
    private String institutionDetails;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "orgntype")
    private String orgnType;

    @Column(name = "enterprisetype")
    private String enterpriseType;

    @Column(name = "natureOfbusiness")
    private String natureOfBusiness;

    @Column(name = "productdesc")
    private String productDesc;

    @Column(name = "registrationunder")
    private String registrationUnder;

    @Column(name = "registrationno")
    private String registrationNo;

    @Column(name = "dateOfregistration")
    private String dateOfRegistration;

    @Column(name = "udyamregistrationno")
    private String udyamRegistrationNo;

    @Column(name = "niccode")
    private String nicCode;

    @Column(name = "incorporationdate")
    private String incorporationDate;

    @Column(name = "commmencedate")
    private String commenceDate;

    @Column(name = "udyamAadharrgistrationno")
    private String udyamAadharRegistrationNo;

    @Column(name = "gstregno")
    private String gstRegNo;

    @Column(name = "din")
    private String din;

    @Column(name = "photograph")
    private String photograph;

    @Column(name = "unitholderorownername")
    private String unitHolderOrOwnerName;

    @Column(name = "firstmiddlelastName")
    private String firstMiddleLastName;

    @Column(name = "designation")
    private String designation;

    @Column(name = "caste")
    private String caste;

    @Column(name = "specialcategory")
    private String specialCategory;

    @Column(name = "gender")
    private String gender;

    @Column(name = "dateofbirth")
    private String dateOfBirth;

    @Column(name = "qualification")
    private String qualification;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "pan")
    private String pan;

    @Column(name = "aadharno")
    private String aadharNo;

    @Column(name = "passportno")
    private String passportNo;

    @Column(name = "communicationaddress")
    private String communicationAddress;

    @Column(name = "commDoorno")
    private String commDoorNo;

    @Column(name = "commlocality")
    private String commLocality;

    @Column(name = "commstreet")
    private String commStreet;

    @Column(name = "commlandmark")
    private String commLandmark;

    @Column(name = "commNameofthebuilding")
    private String commNameOfTheBuilding;

    @Column(name = "floorno")
    private String floorNo;

    @Column(name = "commvillage")
    private String commVillage;

    @Column(name = "commmandal")
    private String commMandal;

    @Column(name = "commdistrict")
    private String commDistrict;

    @Column(name = "commpincode")
    private String commPinCode;

    @Column(name = "commmobileno")
    private String commMobileNo;

    @Column(name = "commalternateno")
    private String commAlternateNo;

    @Column(name = "emailaddress")
    private String emailAddress;

    @Column(name = "ltht")
    private String ltHt;

    @Column(name = "loadkva")
    private String loadKva;

    @Column(name = "serviceno")
    private String serviceNo;

    @Column(name = "currentstatus")
    private String currentStatus;

    @Column(name = "unitcostoriinvestment")
    private String unitCostOrInvestment;

    @Column(name = "netturnoverrupees")
    private String netTurnoverRupees;

    @Column(name = "typeofloan")
    private String typeOfLoan;

    @Column(name = "sourceofloan")
    private String sourceOfLoan;

    @Column(name = "loanapplieddate")
    private String loanAppliedDate;

    @Column(name = "loansanctiondate")
    private String loanSanctionDate;

    @Column(name = "subsidyapplicationdate")
    private String subsidyApplicationDate;

    @Column(name = "bankname")
    private String bankName;

    @Column(name = "branchnameaddress")
    private String branchNameAddress;

    @Column(name = "ifsccode")
    private String ifscCode;

    @Column(name = "releasedatedoc")
    private String releaseDateDoc;

    @Column(name = "workingcapital")
    private String workingCapital;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "firmregyear")
    private String firmRegYear;
}

