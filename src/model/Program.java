package model;


public class Program {
 public enum Category { FN, MK, AC, HRM, OM }
 public enum InterestLevel { LOW(1), MEDIUM(2), HIGH(3), VERY_HIGH(4);
     private final int rank;
     InterestLevel(int r){ rank = r; }
     public int rank(){ return rank; }
     public static InterestLevel fromString(String s){
         switch(s.toLowerCase()){
             case "low": return LOW;
             case "medium": return MEDIUM;
             case "high": return HIGH;
             case "very high":
             case "very_high":
             case "veryhigh": return VERY_HIGH;
             default: return LOW;
         }
     }
 }

 private final String name;
 private final Category category;
 private final double minIndustrySalary; // SAR
 private final double minRequiredPreviousGPA;
 private final InterestLevel analyticalInterestRequired;
 private final double requiredAcceptableGPAAfterDegree; // industry acceptable after degree

 public Program(String name, Category category, double minIndustrySalary,
                double minRequiredPreviousGPA, InterestLevel analyticalInterestRequired,
                double requiredAcceptableGPAAfterDegree) {
     this.name = name;
     this.category = category;
     this.minIndustrySalary = minIndustrySalary;
     this.minRequiredPreviousGPA = minRequiredPreviousGPA;
     this.analyticalInterestRequired = analyticalInterestRequired;
     this.requiredAcceptableGPAAfterDegree = requiredAcceptableGPAAfterDegree;
 }

 // Getters
 public String getName(){ return name; }
 public Category getCategory(){ return category; }
 public double getMinIndustrySalary(){ return minIndustrySalary; }
 public double getMinRequiredPreviousGPA(){ return minRequiredPreviousGPA; }
 public InterestLevel getAnalyticalInterestRequired(){ return analyticalInterestRequired; }
 public double getRequiredAcceptableGPAAfterDegree(){ return requiredAcceptableGPAAfterDegree; }

 @Override
 public String toString(){
     return String.format("%s (%s) - MinSal: SAR %.0f, RequiredPrevGPA: %.2f, Analytic:%s, PostDegreeGPA: %.2f",
             name, category, minIndustrySalary, minRequiredPreviousGPA,
             analyticalInterestRequired, requiredAcceptableGPAAfterDegree);
 }
}

