package model;

import java.util.*;

public class ProgramRepository {
 private static ProgramRepository instance;
 private final List<Program> programs = new ArrayList<>();

 private ProgramRepository(){
     loadDefaults();
 }

 public static synchronized ProgramRepository getInstance(){
     if(instance == null) instance = new ProgramRepository();
     return instance;
 }

 private void loadDefaults(){
     // The spec requires using all categories and one or more programs each.
     // Using the canonical five categories from the spec (names are examples).
     programs.add(ProgramFactory.create("Finance - Financial Analyst", Program.Category.FN, 5000, 3.0, Program.InterestLevel.LOW, 3.5));
     programs.add(ProgramFactory.create("Finance - Corporate Finance", Program.Category.FN, 5500, 3.0, Program.InterestLevel.LOW, 3.5));

     programs.add(ProgramFactory.create("Marketing - Digital Marketing", Program.Category.MK, 7000, 3.5, Program.InterestLevel.VERY_HIGH, 4.0));
     programs.add(ProgramFactory.create("Marketing - Brand Management", Program.Category.MK, 7200, 3.5, Program.InterestLevel.VERY_HIGH, 4.0));

     programs.add(ProgramFactory.create("Accounting - Audit & Assurance", Program.Category.AC, 5000, 3.0, Program.InterestLevel.HIGH, 3.5));
     programs.add(ProgramFactory.create("Accounting - Management Accounting", Program.Category.AC, 5200, 3.0, Program.InterestLevel.HIGH, 3.5));

     programs.add(ProgramFactory.create("HRM - HR Specialist", Program.Category.HRM, 5000, 3.0, Program.InterestLevel.HIGH, 3.5));
     programs.add(ProgramFactory.create("HRM - Organizational Development", Program.Category.HRM, 5400, 3.0, Program.InterestLevel.HIGH, 3.5));

     programs.add(ProgramFactory.create("Operations - Operations Analyst", Program.Category.OM, 6000, 3.5, Program.InterestLevel.MEDIUM, 3.5));
     programs.add(ProgramFactory.create("Operations - Supply Chain Management", Program.Category.OM, 6200, 3.5, Program.InterestLevel.MEDIUM, 3.5));
 }

 public List<Program> getPrograms(){ return Collections.unmodifiableList(programs); }

 // Optionally add method to add program at runtime (not used now)
 public void addProgram(Program p){ programs.add(p); }
}

