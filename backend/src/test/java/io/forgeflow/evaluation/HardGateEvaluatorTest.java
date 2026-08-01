package io.forgeflow.evaluation;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class HardGateEvaluatorTest {
 private HardGateEvaluator.Input pass(){return new HardGateEvaluator.Input(true,true,true,true,true,true,true,true,true,true,true,true,true,true);}
 @Test void noScoreIsProducedWhenAnyGateFails(){var i=pass();i=new HardGateEvaluator.Input(true,true,true,true,true,true,true,true,true,false,true,true,true,true);var result=new HardGateEvaluator().evaluate(i);assertFalse(result.passed());assertTrue(result.score().isEmpty());assertTrue(result.failures().contains("REQUIRED_TEST_FAILED"));}
 @Test void allGatesPassEntersHumanTestScoreBand(){var result=new HardGateEvaluator().evaluate(pass());assertTrue(result.passed());assertEquals(99,result.score().orElseThrow());}
}
