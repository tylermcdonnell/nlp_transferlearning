import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.FileNotFoundException;

import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.parser.lexparser.Options.LexOptions;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.ling.*;

public class WSJPreprocessor
{
    public static void main(String[] args)
    {
	WSJPreprocessor wsjp = new WSJPreprocessor();
	wsjp.buildTrainSet("/media/1tb/tyler/cs/data/annotated_parse_trees/brown_test/", 1000, "test.txt");

	Treebank test = wsjp.generateTreebank();
	test.loadPath("/media/1tb/tyler/cs/Java/stanford-parser-full-2015-12-09/test.txt");
	System.out.println(test.size());
    }

    public Treebank generateTreebank()
    {
	// Load as Treebank for easy separation.
	Options options = new Options();
	options.doDep = false;
	options.doPCFG = true;
	options.setOptions("-goodPCFG", "-evals", "tsv");
	return options.tlpParams.testMemoryTreebank();
    }
    
    public void writeToFile(Treebank treebank, String filename)
    {
	TreePrint printer = new TreePrint("penn");
	try (PrintWriter out = new PrintWriter( "test.txt" ))
	{
	    for (Tree t : treebank)
	    {
		printer.printTree(t, out);
	    }
	}
	catch (FileNotFoundException exc)
	{
	    System.out.println("FileNotFoundException.");
	}	
    }

    public void buildTrainSet(String path, int trainSentences, String out)
    {
	// Load entirety.
	Treebank wsjData = generateTreebank();
	wsjData.loadPath(path);

	// Build train set of specified size.
	Treebank train = generateTreebank();
	Iterator<Tree> it = wsjData.iterator();
	int index = 0;
	while (it.hasNext() && index < trainSentences)
	{
	    Tree next = it.next();
	    train.add(next);
	    index += 1;
	}
	
	writeToFile(train, out);
    }
}
