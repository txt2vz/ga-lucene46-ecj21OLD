package query;

import java.io.IOException;

import lucene.ImportantWords;
import lucene.IndexInfoStaticG;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;

import ec.EvolutionState;
import ec.Individual;
import ec.Problem;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import ec.vector.IntegerVectorIndividual;


public class ClassifyAndOrNotGA extends Problem implements SimpleProblemForm {

	private IndexSearcher searcher = IndexInfoStaticG.getIndexSearcher();

	private float F1train = 0;

	private String[] wordArrayPos, wordArrayNeg;

	private BooleanQuery query;

	public void setup(final EvolutionState state, final Parameter base) {

		super.setup(state, base);

		try {
			System.out.println("Total docs for cat  "
					+ IndexInfoStaticG.getCatnumberAsString() + " "
					+ IndexInfoStaticG.totalTrainDocsInCat
					+ " Total test docs for cat "
					+ IndexInfoStaticG.totalTestDocsInCat);

			ImportantWords iw = new ImportantWords();
			wordArrayPos = iw.getF1WordList(false, true);
			wordArrayNeg = iw.getF1WordList(false, false);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void evaluate(final EvolutionState state, final Individual ind,
			final int subpopulation, final int threadnum) {

		if (ind.evaluated)
			return;

		GAFit fitness = (GAFit) ind.fitness;

		IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind;

		query = new BooleanQuery(true);
		for (int i = 0; i < (intVectorIndividual.genome.length - 1); i = i + 2) {

			if (intVectorIndividual.genome[i] < 0
					|| intVectorIndividual.genome[i] >= wordArrayPos.length
					|| intVectorIndividual.genome[i] >= wordArrayNeg.length
					|| intVectorIndividual.genome[i + 1] < 0
					|| intVectorIndividual.genome[i + 1] >= wordArrayPos.length
					|| intVectorIndividual.genome[i + 1] >= wordArrayNeg.length)
				continue;

			int wordInd = intVectorIndividual.genome[i + 1];

			switch (intVectorIndividual.genome[i]) {
			case 0:
				query.add(
						new TermQuery(new Term(IndexInfoStaticG.FIELD_CONTENTS,
								wordArrayPos[wordInd])),
						BooleanClause.Occur.SHOULD);
				break;
			case 1:
				query.add(
						new TermQuery(new Term(IndexInfoStaticG.FIELD_CONTENTS,
								wordArrayPos[wordInd])),
						BooleanClause.Occur.MUST);
				break;
			case 2:
				query.add(
						new TermQuery(new Term(IndexInfoStaticG.FIELD_CONTENTS,
								wordArrayNeg[wordInd])),
						BooleanClause.Occur.MUST_NOT);
				break;
			default:
				query.add(
						new TermQuery(new Term(IndexInfoStaticG.FIELD_CONTENTS,
								wordArrayPos[wordInd])),
						BooleanClause.Occur.SHOULD);
			}
		}

		try {
			TotalHitCountCollector collector = new TotalHitCountCollector();
			searcher.search(query, IndexInfoStaticG.catTrainF, collector);
			final int positiveMatch = collector.getTotalHits();

			collector = new TotalHitCountCollector();
			searcher.search(query, IndexInfoStaticG.othersTrainF, collector);
			final int negativeMatch = collector.getTotalHits();

			F1train = ClassifyQuery.f1(positiveMatch, negativeMatch,
					IndexInfoStaticG.totalTrainDocsInCat);

			fitness.setTrainValues(positiveMatch, negativeMatch);
			fitness.setF1Train(F1train);
			fitness.setQuery(query);

		} catch (IOException e) {

			e.printStackTrace();
		}

		float rawfitness = F1train;

		((SimpleFitness) intVectorIndividual.fitness).setFitness(state,
				rawfitness, false);

		ind.evaluated = true;
	}
}
