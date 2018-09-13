/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.design;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeBodyDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;
import net.sourceforge.pmd.lang.java.metrics.impl.visitors.AtfmAttributeAccessCollector;
import net.sourceforge.pmd.lang.java.metrics.impl.visitors.AtlmAttributeAccessCollector;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.metrics.MetricOptions;
import net.sourceforge.pmd.util.StringUtil;


public class FeatureEnvyJDeodorantRule extends AbstractJavaRule {


    @Override
    public Object visit(ASTClassOrInterfaceDeclaration node, Object data) {
        HashMap<String,Double> envyTarget = findEnvyTarget(computeFor(node));
        super.visit(node, data);
        if (envyTarget!=null && envyTarget.size()>0) {
            Gson gson = new Gson();
            String json = gson.toJson(envyTarget);
//            Double intensity = 0.0;
//            List<String> arr = new ArrayList<>();
//            for(Map.Entry<String,Double> e: envyTarget.entrySet()){
//                intensity = e.getValue();
//                arr.add(e.getKey());
//                message.append(e.getKey()).append(" ");
//            }
//            message.append("| ").append(intensity);
            addViolationWithMessage(data, node, json);
        }
        return data;
    }

    private int getMethodDeclarationSize(ASTAnyTypeDeclaration node){
        int res = 0;
        for(ASTAnyTypeBodyDeclaration bd : node.getDeclarations()){
            if(bd.getKind().equals(ASTAnyTypeBodyDeclaration.DeclarationKind.METHOD) ||
                    bd.getKind().equals(ASTAnyTypeBodyDeclaration.DeclarationKind.CONSTRUCTOR)){
                res++;
            }
        }
        return res;
    }
    public Map<String, Map<String,Double>>  computeFor(ASTAnyTypeDeclaration node) {
        Map<String, Set<String>> atlmByMethod = new AtlmAttributeAccessCollector(node).start();
        Map<String, Map<String,Set<String>>> atfmByMethod = new AtfmAttributeAccessCollector(node).start();
        if(atfmByMethod.size() == 0) return null;
        Map<String, Map<String,Double>> result = new HashMap<>();
        for(Map.Entry<String,Map<String,Set<String>>> e : atfmByMethod.entrySet()){
            Set<String> atlmSet = atlmByMethod.get(e.getKey());
            int atlm = atlmSet == null ? 0 : atlmSet.size();
            Map<String,Set<String>> atfm = e.getValue();
            if(atfm == null || atfm.size()<=0) continue;
//            atfm = findTopAtfm(atfm);
            boolean shouldContinue = true;
            Map<String,Double> res = new HashMap<>();
            for(Map.Entry<String,Set<String>> eInner:atfm.entrySet()){
                int atfmN = eInner.getValue().size();
                int nMethods = getMethodDeclarationSize(node);
                double intensity = nMethods == 0 ? 0 : ((atfmN - atlm) / (nMethods+0.0));
                if(intensity <= 0){
                    shouldContinue = false;break;
                }
                res.put(eInner.getKey(),intensity);
            }
            if(!shouldContinue) continue;
            if(res.size()>0) {
                result.put(e.getKey(), res);
            }
        }
        return result;
    }

    private Map<String,Set<String>> findTopAtfm(Map<String,Set<String>> atfm){
        int max = -1;
        Map<String,Set<String>> res = new HashMap<>();
        for(Map.Entry<String,Set<String>> e : atfm.entrySet()){
            int c = e.getValue().size();
            if(c > max) max = c;
        }
        for(Map.Entry<String,Set<String>> e : atfm.entrySet()){
            int c = e.getValue().size();
            if(c == max) res.put(e.getKey(),e.getValue());
        }
        return res;
    }

    private HashMap<String,Double> findEnvyTarget(Map<String, Map<String,Double>> res){
        if(res == null || res.size() == 0) return null;
        HashMap<String,Double> candidates = new HashMap<>();
        for(Map.Entry<String, Map<String,Double>> firstLevel: res.entrySet()){
            String targetMethod = firstLevel.getKey();
            for(Map.Entry<String,Double> secondLevel : firstLevel.getValue().entrySet()){
                String enviedClass = secondLevel.getKey();
                boolean shouldContinue = false;
//                for(String od : onDemand){
//                    if(od.contains(enviedClass)){
//                        shouldContinue = true;
//                    }
//                }
//                if(!shouldContinue){
//                    continue;
//                }
                Double intensity = candidates.get(enviedClass) == null ? 0 : candidates.get(enviedClass);
                intensity+=secondLevel.getValue();
                candidates.put(enviedClass,intensity);
            }
        }
        double max = -1;
        HashMap<String, Double> result = new HashMap<>();
        for(Map.Entry<String, Double> e: candidates.entrySet()){
            if(max<e.getValue()) max = e.getValue();
        }
        for(Map.Entry<String, Double> e: candidates.entrySet()){
            if(max==e.getValue()) result.put(e.getKey(),e.getValue());
        }
        return result;
    }

}
