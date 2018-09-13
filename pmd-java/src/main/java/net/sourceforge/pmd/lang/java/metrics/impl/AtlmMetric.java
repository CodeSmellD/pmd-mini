/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.metrics.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.MethodLikeNode;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey;
import net.sourceforge.pmd.lang.java.metrics.impl.visitors.AtfmAttributeAccessCollector;
import net.sourceforge.pmd.lang.java.metrics.impl.visitors.AtlmAttributeAccessCollector;
import net.sourceforge.pmd.lang.java.metrics.impl.visitors.TccAttributeAccessCollector;
import net.sourceforge.pmd.lang.metrics.MetricOptions;
import net.sourceforge.pmd.lang.metrics.ResultOption;

/**
 * Access to Foreign Data. Quantifies the number of foreign fields accessed directly or via accessors.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class AtlmMetric extends AbstractJavaClassMetric{

        @Override
        public double computeFor(ASTAnyTypeDeclaration node, MetricOptions options) {
            Map<String, Set<String>> atlmByMethod = new AtlmAttributeAccessCollector(node).start();
            Map<String, Map<String,Set<String>>> atfmByMethod = new AtfmAttributeAccessCollector(node).start();
            Map<String, Map<String,Integer>> result = new HashMap<>();
            for(Map.Entry<String,Set<String>> e : atlmByMethod.entrySet()){
                int atlm = e.getValue().size();
                Map<String,Set<String>> atfm = atfmByMethod.get(e.getKey());
                if(atfm == null) continue;
                atfm = findTopAtfm(atfm);
                boolean shouldContinue = true;
                Map<String,Integer> res = new HashMap<>();
                for(Map.Entry<String,Set<String>> eInner:atfm.entrySet()){
                    int atfmN = eInner.getValue().size();
                    if(atfmN - atlm <= 0){
                        shouldContinue = false;break;
                    }
                    res.put(eInner.getKey(),atfmN - atlm);
                }
                if(!shouldContinue) continue;
                result.put(e.getKey(),res);
            }
            return result.size();
        }

        private Map<String,Set<String>> findTopAtfm(Map<String,Set<String>> atfm){
            return null;
        }


}
