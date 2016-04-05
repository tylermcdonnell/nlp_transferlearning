import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileFilter;

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
	String path = args[0];
	
	// Load WSJ train and test sets in entirety.
	Treebank wsjTrain = wsjp.generateTreebank();
	wsjTrain.loadPath(path, wsjp.WSJTrainFileFilter());
	Treebank wsjTest  = wsjp.generateTreebank();
	wsjTest.loadPath(path, wsjp.WSJTestFileFilter());

	// Generate WSJ seed sets for out-of-domain training.
	int[] trainSizes = {1000, 2000, 3000, 4000, 5000, 7000, 10000, 13000, 16000, 20000, 25000, 30000, 35000};
	for (int trainSize : trainSizes)
	{
	    String out = String.format("wsjseed%d.txt", trainSize);
	    wsjp.buildTrainSet(wsjTrain, trainSize, out);
	    wsjp.confirmSize(out, trainSize);
	    System.out.println("Build training set of size " + trainSize + ".");
	}

	String out;

	// Generate WSJ test set for in-domain testing.
	System.out.println("Building in-domain test set from Section 23.");
	out = String.format("wsjtestsection23.txt");
	wsjp.buildTrainSet(wsjTest, wsjTest.size(), out);
	wsjp.confirmSize(out, wsjTest.size());

	// Generate WSJ self-training set from Sections 2-22.
	System.out.println("Building inverted test set from Sections 2-22.");
	out = String.format("wsjtestsections2-22.txt");
	wsjp.buildTrainSet(wsjTrain, wsjTrain.size(), out);
	wsjp.confirmSize(out, wsjTrain.size());
    }
   
    public void buildTrainSet(Treebank fullData, int trainSentences, String out)
    {
	// Build train set of specified size.
	Treebank train = generateTreebank();
	Iterator<Tree> it = fullData.iterator();
	int index = 0;
	while (it.hasNext() && index < trainSentences)
	{
	    Tree next = it.next();
	    train.add(next);
	    index += 1;
	}
	
	writeToFile(train, out);
    }

    public FileFilter WSJTrainFileFilter()
    {
	return new WSJTrainFileFilter();
    }

    public FileFilter WSJTestFileFilter()
    {
	return new WSJTestFileFilter();
    }

    private class WSJTrainFileFilter implements FileFilter
    {
	// Arbitrarily, we defined SEctions 2-22 as the train source.
	public boolean accept(File pathname)
	{
	    return pathname.toString().substring(4,6).equals("23") == false &&
		   pathname.toString().substring(4,6).equals("24") == false;
	}
    }

    private class WSJTestFileFilter implements FileFilter
    {
	// Arbitrarily, we define Section 23 as the test source.
	public boolean accept(File pathname)
	{
	    return pathname.toString().substring(4,6).equals("23");
	}
    }

    public void confirmSize(String path, int size)
    {
	Treebank confirm = this.generateTreebank();
	confirm.loadPath(path);
	assert (confirm.size() == size);	
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
	try (PrintWriter out = new PrintWriter( filename ))
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
}
