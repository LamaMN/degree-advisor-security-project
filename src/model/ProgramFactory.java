package model;

public class ProgramFactory {
	public static Program create(String name, String category, double minSalary, double prevGpaReq,
			Program.InterestLevel interestReq, double postDegreeGpaReq) {
		return new Program(name, category, minSalary, prevGpaReq, interestReq, postDegreeGpaReq);
	}
}

