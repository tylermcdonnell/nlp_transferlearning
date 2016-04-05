import java.util.List;
import java.util.LinkedList;

import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.parser.lexparser.Options.LexOptions;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.ling.*;

public class DomainAdaptation
{
    // The final trained parser. NULL if no parser trained.
    private LexicalizedParser parser;

    // The static options used for each parser.
    public Options parserOptions;

    /**
     * API for unsupervised domain adaptation.
     * 
     * Domain adaptation, or transfer learning, involves adapting a system trained
     * on one source domain to perform better on a target domain, which may be 
     * different in nature from the source. This approach to domain adaptation 
     * trains a model using the labeled source domain data and uses the learned
     * model to label a large amount of out-of-domain data in the target domain.
     * Then, the labeled out-of-domain (or self-training) data is combined with
     * the source domain data to form the learning basis for a new model. The final
     * model is then applied to label new instances in the target domain.
     *
     * Usage: {@code java DomainAdaptation [[seed] [selfTraining] [test]]}
     */
    public static void main(String[] args)
    {
	String seedSet = args[0];
	String selfTrainingSet = args[1];
	String testSet = args[2];
	System.out.println("---------------------------------");
	System.out.println("Running Domain Adaptation");
	System.out.println("---------------------------------");
	System.out.println(String.format("Model: %s", "Good PCFG"));
	System.out.println(String.format("Seed Set: %s", seedSet));
	System.out.println(String.format("Self-Training Set: %s", selfTrainingSet));
	System.out.println(String.format("Test Set: %s", testSet));

	DomainAdaptation model = new DomainAdaptation();

	// We can easily add hooks for other options through CLI.
        Options options = new Options();
	options.doDep = false;
	options.doPCFG = true;
	options.setOptions("-goodPCFG", "-evals", "tsv");
	model.parserOptions = options;

	if (args.length > 0) 
	{
	    model.train(seedSet, selfTrainingSet);
	    System.out.println("Finished domaina adaptation. Beginning test.");
	    model.test(testSet);
	}
    }

    public void train(String seedPath, String selfTrainPath)
    {
	// Train on seed set.
	Treebank seed = this.parserOptions.tlpParams.testMemoryTreebank();
	seed.loadPath(seedPath);
	System.out.println(seed.toString());
	LexicalizedParser seedParser = LexicalizedParser.trainFromTreebank(seed, this.parserOptions);

	// Extract text from self-training set.
	Treebank selfTraining = this.parserOptions.tlpParams.testMemoryTreebank();
	selfTraining.loadPath(selfTrainPath);
	LinkedList<List<? extends HasWord>> selfTrainingUnlabeled = new LinkedList<>();
	for (Tree t : selfTraining) 
	{
	    List<? extends HasWord> sentence = getInputSentence(t);
	    selfTrainingUnlabeled.add(getInputSentence(t));
	}

	// Label self-training set.
	List<Tree> selfTrainingLabeled = seedParser.parseMultiple(selfTrainingUnlabeled);

	// Concatenate seed and self-training data for transfer learning.
	Treebank trainSet = seed;
	trainSet.addAll(selfTrainingLabeled);

	// Train with concatenated dataset.
	this.parser = LexicalizedParser.trainFromTreebank(trainSet, this.parserOptions);
    }

    public void test(String testPath)
    {
	Treebank test = this.parserOptions.tlpParams.testMemoryTreebank();
	test.loadPath(testPath);
	EvaluateTreebank evaluator = new EvaluateTreebank(this.parser);
	evaluator.testOnTreebank(test);
	
    }

    /**
     * Returns the input sentence for the parser.
     */
    private List<? extends HasWord> getInputSentence(Tree t) {
	return t.yieldWords();
    }

    
}
