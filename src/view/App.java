package view;


import javax.swing.SwingUtilities;

import logic.RecommendationEngine;
import model.ProgramRepository;

public class App {
 public static void main(String[] args){
     var repo = ProgramRepository.getInstance();
     var engine = new RecommendationEngine(repo.getPrograms());

     SwingUtilities.invokeLater(() -> {
         var frame = new MainFrame(engine);
         frame.setVisible(true);
     });
 }
}
