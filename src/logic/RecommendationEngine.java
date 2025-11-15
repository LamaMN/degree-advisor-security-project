package logic;


import java.util.*;
import java.util.stream.Collectors;

import model.Program;

public class RecommendationEngine {
 private final List<Program> programs;

 public RecommendationEngine(List<Program> programs){
     this.programs = programs;
 }

 public static class Input {
     public final double minAcceptableSalary;
     public final double previousGPA;
     public final Program.InterestLevel studentInterest;

     public Input(double minAcceptableSalary, double previousGPA, Program.InterestLevel studentInterest){
         this.minAcceptableSalary = minAcceptableSalary;
         this.previousGPA = previousGPA;
         this.studentInterest = studentInterest;
     }
 }

 public static class Recommendation {
     public final Program program;
     public final double suggestedExtraStudyHours; // per day
     public Recommendation(Program program, double suggestedExtraStudyHours){
         this.program = program;
         this.suggestedExtraStudyHours = suggestedExtraStudyHours;
     }
 }

 public List<Recommendation> recommend(Input input){
     // Filter logic per spec:
     // - program.minIndustrySalary >= input.minAcceptableSalary
     // - input.previousGPA >= program.minRequiredPreviousGPA
     // - input.studentInterest.rank >= program.analyticalInterestRequired.rank
     List<Recommendation> results = programs.stream()
         .filter(p -> p.getMinIndustrySalary() >= input.minAcceptableSalary)
         .filter(p -> input.previousGPA >= p.getMinRequiredPreviousGPA())
         .filter(p -> input.studentInterest.rank() >= p.getAnalyticalInterestRequired().rank())
         .map(p -> {
             // study hours calculation: additional hours to reach post-degree required GPA
             double gap = p.getRequiredAcceptableGPAAfterDegree() - input.previousGPA;
             double extraHours = Math.max(0.0, gap); // 1 hour per 1 GPA point
             // round to two decimals for display
             extraHours = Math.round(extraHours * 100.0)/100.0;
             return new Recommendation(p, extraHours);
         })
         .collect(Collectors.toList());

     return results;
 }
}

