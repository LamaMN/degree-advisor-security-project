package model;

public class ProgramFactory {
 public static Program create(String name, Program.Category cat, double minSalary,
                              double prevGpaReq, Program.InterestLevel interestReq,
                              double postDegreeGpaReq) {
     return new Program(name, cat, minSalary, prevGpaReq, interestReq, postDegreeGpaReq);
 }
}

