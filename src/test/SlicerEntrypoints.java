package test;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.ArgumentTypeEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Created by markus on 23.02.14.
 */
public class SlicerEntrypoints extends HashSet<Entrypoint> {


    private final static boolean DEBUG = true;

    private static final Logger log = Logger.getLogger(SlicerEntrypoints.class.getName());

    /**
     * @param scope governing analyais scope
     * @param cha governing class hierarchy
     * @throws IllegalArgumentException if cha is null
     */
    public SlicerEntrypoints(AnalysisScope scope, final IClassHierarchy cha, String className) {

        if (cha == null) {
            throw new IllegalArgumentException("cha is null");
        }
        log.info(className);
        boolean classInCha = false;
        for (IClass klass : cha) {
            if (!klass.isInterface()) {
                String typname = klass.getName().toString();
                if(className.contentEquals(typname)) {
                    if (isApplicationClass(scope, klass)) {
                        for (Iterator methodIt = klass.getDeclaredMethods().iterator(); methodIt.hasNext();) {
                            IMethod method = (IMethod) methodIt.next();
                            if (!method.isAbstract() && method.isPublic()) {
                                add(new ArgumentTypeEntrypoint(method, cha));
                            }
                        }
                    }
                }

            }
        }
        if (DEBUG) {
            System.err.println((getClass() + "Number of EntryPoints:" + size()));
        }

    }

    /**
     * @return true iff klass is loaded by the application loader.
     */
    private boolean isApplicationClass(AnalysisScope scope, IClass klass) {
        return scope.getApplicationLoader().equals(klass.getClassLoader().getReference());
    }
}