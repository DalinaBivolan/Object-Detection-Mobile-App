package disertatie.od.customview;

import java.util.List;
import disertatie.od.tflite.Classifier.Recognition;

public interface ResultsView {
  public void setResults(final List<Recognition> results);
}
