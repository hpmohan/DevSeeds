/* Extension for Getiing Product price details from standard price books */

global without sharing class PricingDetails {
    RegistrationProductSelection__c regprsel{get;set;}
    global String product2ID{get;set;}
    global String price{get;set;}
    global PriceBookEntry selectedPriceBook{get;set;}
    
    private final RegistrationStage__c regRec;
    
    
    global Id registrationStageId {get;set;}
    public Set<Id> selectedProductIdSet = new Set<Id>();
    public Map<String,Decimal> productNameToUnitPriceMap = new Map<String,Decimal>();
    public RegistrationProductSelection__c registrationProductSelected{get;set;}
    public Decimal totalUnitPrice{get;set;}
    
    // selectedProducts variables
    public boolean isPlatinumPlusSelected{get;set;}
    public boolean isPlatinumSelected{get;set;}
    public boolean isERASelected{get;set;}
    public boolean isClaimsSelected{get;set;}
    public boolean isRealTimeSelected{get;set;}
    public boolean is45017500Selected{get;set;}
    public boolean is7501plusSelected{get;set;}
    public boolean is1150Selected{get;set;}
    public boolean is1514500Selected{get;set;}
    public boolean ispaperClaimsSelected{get;set;}
    public boolean isPatientStmtSelected{get;set;}
    //Claim Type variables
    public boolean CMSselected{get;set;}
    public boolean UBselected{get;set;}
    //unit price varibales
    public Decimal upPlatinumPlus{get;set;}
    public Decimal upPlatinum{get;set;}
    public Decimal upERA{get;set;}
    public Decimal upClaims{get;set;}
    public Decimal upRealTime{get;set;}
    public Decimal up45017500{get;set;}
    public Decimal up7501plus{get;set;}
    public Decimal up1150{get;set;}
    public Decimal up1514500{get;set;}
    public Decimal upPaperCalims{get;set;}
    public Decimal upPatientStmt{get;set;}
    public Decimal upAdditionalPage{get;set;}
    public Decimal upCustomStmt{get;set;}
    public Decimal upSetupFee{get;set;} // always needed.
    //couponPrice if selected
    public Decimal couponPrice= 0;
    public Decimal finalSetupFee{get;set;} // for pdf.
    
    //transaction type variables
    public boolean perTransactionSelected{get;set;}
    public boolean perProviderSelected{get;set;}
    public boolean noCoreServicesSelected{get;set;}
    public boolean additionalServicesSelected{get;set;}
    //Selected channel partner
    public String channelPartnerName{get;set;}
    public String channelPartnerVNId{get;set;}
    public String orgName{get;set;}
    //List of selected products
    public Set<String> selectedProductSet = new Set<String>();
    
    /* Getting ID from URL for selected product from product selection page for price book details of perticular product */
    
    global PricingDetails(ApexPages.StandardController controller) {
        //default channelPartnerName
        channelPartnerName = 'Optum360';
        channelPartnerVNId = '';
        //Claim Types
        CMSselected = false;
        UBselected = false;
        //setting product selected to false
        isPlatinumPlusSelected = false;
        isPlatinumSelected = false;
        isERASelected = false;
        isClaimsSelected= false;
        isRealTimeSelected = false;
        is45017500Selected = false;
        is7501plusSelected = false;
        is1150Selected = false;
        is1514500Selected = false;
        ispaperClaimsSelected= false;
        isPatientStmtSelected = false;
        //Initializing transaction type variables
        perTransactionSelected = false;
        perProviderSelected = false;
        noCoreServicesSelected = false;
        additionalServicesSelected = false;
        if(ApexPages.CurrentPage() != null){
            registrationStageId  = ApexPages.CurrentPage().getparameters().get('id');
        }
        else if(registrationStageId == null){
            this.regRec = (RegistrationStage__c)controller.getRecord();
            System.debug('==regRec=='+regRec);
            registrationStageId = regRec.Id;
        }
        System.debug('===registrationStageId==='+registrationStageId);
        if(String.isNotBlank(registrationStageId)){
            System.debug('===Not Blank==='+registrationStageId);
            //get price details of all products.
            //productNameToUnitPriceMap = 
            
            totalUnitPrice = 0;
            // Apex CRUD validation
            RegistrationProductSelection__c rpsAccess = new RegistrationProductSelection__c();
            if(AccessCheck.isAccessible(rpsAccess)){
                
                
                
                List<RegistrationProductSelection__c> productSelectionList =  [SELECT id, product__c, product__r.Name, product__r.ProductCode
                                                                               , Product_Family__c, Intelligent_EDI__c
                                                                               , Primary_format_and_submission_method__c
                                                                               , Additional_Services__c
                                                                               , Core_Intelligent_EDI_Services__c
                                                                               , Claims_Type__c , FormatOptions__c 
                                                                               , Transaction_Type__c
                                                                               , Registration_Stage__r.Organization_Name__c
                                                                               , Registration_Stage__r.Coupon_ID_Number__c
                                                                               , Registration_Stage__r.Channel_Partner_Account__c
                                                                               , Registration_Stage__r.Channel_Partner_Account__r.Name
                                                                               , Registration_Stage__r.Channel_Partner_Account__r.VN_ID__c
                                                                               , Registration_Stage__r.Practicing_Providers__c
                                                                               FROM RegistrationProductSelection__c 
                                                                               WHERE Registration_Stage__c= :registrationStageId];
                System.debug('===productSelectionList==='+productSelectionList);
                if(!productSelectionList.isEmpty()){
                    //for filling details which are common across products
                    registrationProductSelected = productSelectionList[0];
                    fetchPriceDetails();
                    //channelPartnerName
                    if(String.isNotBlank(registrationProductSelected.Registration_Stage__r.Channel_Partner_Account__c)){
                        //get price details
                        fetchPriceDetails(registrationProductSelected.Registration_Stage__r.Channel_Partner_Account__c);
                        channelPartnerVNId = registrationProductSelected.Registration_Stage__r.Channel_Partner_Account__r.VN_ID__c;
                        channelPartnerName = registrationProductSelected.Registration_Stage__r.Channel_Partner_Account__r.Name;
                    }
                    if(String.isNotBlank(registrationProductSelected.Registration_Stage__r.Coupon_ID_Number__c)){
                        //get coupon price
                        Couponcodes__c selectedCoupon = Couponcodes__c.getInstance(registrationProductSelected.Registration_Stage__r.Coupon_ID_Number__c);
                        couponPrice = selectedCoupon.CouponPrice__c;
                    } 
                    // for download pdf name
                    if(String.isNotBlank(registrationProductSelected.Registration_Stage__r.Organization_Name__c)){
                        //get coupon price
                        orgName= registrationProductSelected.Registration_Stage__r.Organization_Name__c;
                        
                    }
                    //
                    //identifying selected products
                    for(RegistrationProductSelection__c rps: productSelectionList){
                        //claim type variables
                        if(String.isNotBlank(rps.Claims_Type__c)){
                            if(rps.Claims_Type__c.containsIgnoreCase('CMS')){
                                CMSselected = true;
                            }
                            else if(rps.Claims_Type__c.containsIgnoreCase('UB')){
                                UBselected = true;
                            }
                        }
                        //setting transaction type variables
                        if(rps.Transaction_Type__c == 'Per Provider'){
                            perProviderSelected = TRUE;
                        }
                        else if(rps.Transaction_Type__c == 'Per Transaction'){
                            perTransactionSelected = TRUE;
                        }
                        else if(rps.Transaction_Type__c == 'No Core Services'){
                            noCoreServicesSelected = TRUE;
                        }
                        else if(rps.Transaction_Type__c == 'Additional Services'){
                            additionalServicesSelected = TRUE;
                        }
                        //setting selected products variables.
                        if(String.isNotBlank(rps.product__c)){
                            selectedProductIdSet.add(rps.product__c);
                            selectedProductSet.add(rps.product__r.Name);
                            if(rps.product__r.Name =='Platinum Plus'){
                                isPlatinumPlusSelected =  true;
                                upPlatinumPlus = productNameToUnitPriceMap.get(rps.product__r.ProductCode);
                                totalUnitPrice += upPlatinumPlus;
                            }
                            else if(rps.product__r.Name =='Platinum'){
                                isPlatinumSelected =  true;
                                upPlatinum = productNameToUnitPriceMap.get(rps.product__r.ProductCode);
                                System.debug('==upPlatinum=='+upPlatinum);
                                totalUnitPrice += upPlatinum;
                            }
                            else if(rps.product__r.Name.containsIgnoreCase('ERA')){
                                isERASelected =  true;
                                upERA = productNameToUnitPriceMap.get(rps.product__r.ProductCode);
                                System.debug('===upERA==='+upERA);
                                totalUnitPrice += upERA;
                            }
                            else if(rps.product__r.Name.containsIgnoreCase('EDI Claims')){
                                isClaimsSelected= true;
                                upClaims = productNameToUnitPriceMap.get(rps.product__r.ProductCode);
                                System.debug('===upClaims==='+upClaims);
                                totalUnitPrice += upClaims;
                            }
                            else if(rps.product__r.Name.containsIgnoreCase('Real-Time')){
                                isRealTimeSelected = true;
                                upRealTime = productNameToUnitPriceMap.get(rps.product__r.ProductCode);
                                System.debug('===upRealTime==='+upRealTime);
                                totalUnitPrice += upRealTime;
                            }
                            else if(rps.product__r.Name =='1-150'){
                                is1150Selected = true;
                                up1150 = productNameToUnitPriceMap.get(rps.product__r.Name); 
                                up1514500 = productNameToUnitPriceMap.get('151-4,500'); 
                                up45017500 = productNameToUnitPriceMap.get('4,501-7,500'); 
                                up7501plus = productNameToUnitPriceMap.get('7,501+'); 
                            }
                            else if(rps.product__r.Name =='Paper Claims'){
                                ispaperClaimsSelected = true;
                                upPaperCalims = productNameToUnitPriceMap.get(rps.product__r.Name);
                            }
                            else if(rps.product__r.Name =='Patient Statements'){
                                isPatientStmtSelected = true;
                                upPatientStmt = productNameToUnitPriceMap.get(rps.product__r.Name);
                                upAdditionalPage = productNameToUnitPriceMap.get('Additional Page');
                                upCustomStmt = productNameToUnitPriceMap.get('Custom statement');
                            }
                        }
                        
                        
                    }
                    System.debug('----totalUnitPrice----'+totalUnitPrice);
                }
            }
            //always show setup Price
            upSetupFee = productNameToUnitPriceMap.get('Setup Fee');
            if(couponPrice > 0){
                finalSetupFee = upSetupFee - couponPrice;
            }
            else{
                finalSetupFee = upSetupFee;
            }
                
        }
    }
    
    //select standard price all products
    // Old Query
    /* SELECT Pricebook2Id, Pricebook2.Name
                                                   ,Pricebook2.Display_on_portal__c, Product2.name
                                                   ,Pricebook2.IsStandard, Product2Id, UnitPrice, IsActive
                                                   From PricebookEntry 
                                                   WHERE isActive = TRUE 
                                                   AND Pricebook2.Display_on_portal__c =  TRUE*/
    public void fetchPriceDetails(){
        Integer totalProviders = Integer.valueOf(registrationProductSelected.Registration_Stage__r.Practicing_Providers__c);  
        //Map<String,Decimal> prodNameToPriceMap = new Map<String,Decimal>();
        // // Apex CRUD validation
        PricebookEntry pe = new PricebookEntry();
        if(AccessCheck.isAccessible(pe)){
              
         
            List<PricebookEntry> priceBookEnrtyList =  [SELECT Pricebook2Id, Pricebook2.Name, Pricebook2.Display_on_portal__c, 
                                                        Pricebook2.IsStandard, Product2Id, Product2.Name, Product2.ProductCode,  UnitPrice, IsActive,Product2.of_Providers__c 
                                                        From PricebookEntry 
                                                        WHERE (isActive = TRUE AND Pricebook2.Display_on_portal__c = TRUE)
                                                        AND ( Product2.of_Providers__c = 0 OR Product2.of_Providers__c = :totalProviders)];
            if(!priceBookEnrtyList.isEmpty()){
                for(PriceBookEntry pb: priceBookEnrtyList){
                    if(String.isNotBlank(pb.Product2.ProductCode) && pb.Product2.of_Providers__c !=0 ){
                        productNameToUnitPriceMap.put(pb.Product2.ProductCode, pb.UnitPrice);
                    }
                    else{
                        productNameToUnitPriceMap.put(pb.Product2.Name, pb.UnitPrice); 
                    }
                }
                System.debug('==productNameToUnitPriceMap=='+productNameToUnitPriceMap);
            }
        
        }
        //return prodNameToPriceMap;
    }
    
    public void fetchPriceDetails(id accoutId){
        Integer totalProviders = Integer.valueOf(registrationProductSelected.Registration_Stage__r.Practicing_Providers__c);  
        String productName=registrationProductSelected.product__r.Name;
        
        id priceBookId;
        //productNameToUnitPriceMap = new Map<String, Decimal>();
        //get priceBook2Id from Contract based on account Id
        // // Apex CRUD validation
        if(AccessCheck.isAccessible(new Contract())){  
            List<Contract> contractList = [SELECT id, AccountId, Pricebook2Id 
                                           FROM Contract WHERE Status = 'Activated' 
                                           AND AccountId = :accoutId 
                                           AND StartDate <= TODAY AND EndDate >= TODAY 
                                           ORDER BY CreatedDate 
                                           LIMIT 1];
            if(!contractList.isEmpty()){
            priceBookId = contractList[0].Pricebook2Id;
        }
        }
        if(String.isNotBlank(priceBookId)){
            if(AccessCheck.isAccessible(new PricebookEntry())){
              
                    List<PricebookEntry> priceBookEnrtyList = [SELECT Pricebook2Id, Pricebook2.Name, PriceBook2.Account__c, Pricebook2.Display_on_portal__c, 
                                                           Pricebook2.IsStandard, Product2Id, Product2.Name, UnitPrice, IsActive , Product2.ProductCode,Product2.of_Providers__c 
                                                           From PricebookEntry 
                                                           WHERE (isActive = TRUE AND  Pricebook2Id=:priceBookId) AND (Product2.of_Providers__c = :totalProviders
                                                           OR Product2.of_Providers__c = 0)];
                    if(!priceBookEnrtyList.isEmpty()){
                        for(PriceBookEntry pb: priceBookEnrtyList){
                            if(String.isNotBlank(pb.Product2.ProductCode) && pb.Product2.of_Providers__c !=0 ){
                                productNameToUnitPriceMap.put(pb.Product2.ProductCode, pb.UnitPrice); 
                            }
                            else{
                                productNameToUnitPriceMap.put(pb.Product2.Name, pb.UnitPrice); 
                            }
                        }
                        System.debug('==productNameToUnitPriceMap=='+productNameToUnitPriceMap);
                    }
                
                
            }
        }
    }
    // download PDF
    global void forceDownloadPDF(){
        Apexpages.currentPage().getHeaders().put('Content-Disposition', 'attachment; filename='+orgName+'.pdf');
     }
}
