package be.nabu.utils.aspects;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import be.nabu.utils.aspects.api.NotImplemented;

public class AspectUtils {
	
	private static Map<Class<?>, List<Class<?>>> ifaces = new HashMap<Class<?>, List<Class<?>>>();

	/**
	 * This joins multiple objects together into a new object that has all the interfaces of all objects
	 */
	@SuppressWarnings("unchecked")
	public static <T> T join(Object...aspects) {
		Set<Class<?>> interfaces = new HashSet<Class<?>>();
		Map<Method, Object> methodMapping = new HashMap<Method, Object>();
		for (Object aspect : aspects) {
			if (Proxy.isProxyClass(aspect.getClass())) {
				InvocationHandler invocationHandler = Proxy.getInvocationHandler(aspect);
				if (invocationHandler instanceof AspectInstanceInvoker) {
					methodMapping.putAll(((AspectInstanceInvoker) invocationHandler).getMethodMapping());
					interfaces.addAll(getAllInterfaces(aspect.getClass()));
					continue;
				}
			}
			for (Class<?> iface : getAllInterfaces(aspect.getClass())) {
				for (Method method : iface.getMethods()) {
					try {
						Method implementation = aspect.getClass().getMethod(method.getName(), method.getParameterTypes());
						if (implementation.getAnnotation(NotImplemented.class) != null) {
							continue;
						}
					}
					catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
					methodMapping.put(method, aspect);
				}
				interfaces.add(iface);
			}
		}
		return (T) Proxy.newProxyInstance(AspectUtils.class.getClassLoader(), interfaces.toArray(new Class[interfaces.size()]),
			new AspectInstanceInvoker(methodMapping, new ArrayList<Object>(Arrays.asList(aspects))));
	}
	
	private static List<Class<?>> getAllInterfaces(Class<?> clazz) {
		Class<?> originalClass = clazz;
		if (!ifaces.containsKey(clazz)) {
			List<Class<?>> list = new ArrayList<Class<?>>();
			while (clazz != null) {
				for (Class<?> iface : clazz.getInterfaces()) {
					list.add(iface);
				}
				clazz = clazz.getSuperclass();
			}
			if (!ifaces.containsKey(clazz)) {
				synchronized(ifaces) {
					if (!ifaces.containsKey(clazz)) {
						ifaces.put(originalClass, list);
					}
				}
			}
		}
		return ifaces.get(originalClass);
	}
	
	public static boolean hasAspects(Object original) {
		if (!Proxy.isProxyClass(original.getClass())) {
			return false;
		}
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(original);
		while (invocationHandler instanceof CastInstanceInvoker) {
			original = ((CastInstanceInvoker) invocationHandler).getOriginal();
			if (!Proxy.isProxyClass(original.getClass())) {
				return false;
			}
			invocationHandler = Proxy.getInvocationHandler(original);
		}
		if (invocationHandler instanceof AspectInstanceInvoker) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public static List<Object> aspects(Object original) {
		if (Proxy.isProxyClass(original.getClass())) {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(original);
			while (invocationHandler instanceof CastInstanceInvoker) {
				original = ((CastInstanceInvoker) invocationHandler).getOriginal();
				if (!Proxy.isProxyClass(original.getClass())) {
					throw new IllegalArgumentException("The cast object is not an aspect");
				}
				invocationHandler = Proxy.getInvocationHandler(original);
			}
			if (invocationHandler instanceof AspectInstanceInvoker) {
				return ((AspectInstanceInvoker) invocationHandler).getAspects();
			}
			else {
				throw new IllegalArgumentException("The object is a proxy but not an aspect");
			}
		}
		else {
			throw new IllegalArgumentException("The object is not an aspect");
		}
	}
	
	public static Map<Method, Object> mapping(Object original) {
		if (Proxy.isProxyClass(original.getClass())) {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(original);
			while (invocationHandler instanceof CastInstanceInvoker) {
				original = ((CastInstanceInvoker) invocationHandler).getOriginal();
				if (!Proxy.isProxyClass(original.getClass())) {
					throw new IllegalArgumentException("The cast object is not an aspect");
				}
				invocationHandler = Proxy.getInvocationHandler(original);
			}
			if (invocationHandler instanceof AspectInstanceInvoker) {
				return ((AspectInstanceInvoker) invocationHandler).getMethodMapping();
			}
			else {
				throw new IllegalArgumentException("The object is a proxy but not an aspect");
			}
		}
		else {
			throw new IllegalArgumentException("The object is not an aspect");
		}
	}
	
	/**
	 * This adds implementations in place to an object that already has the proper interfaces
	 * @return The list of methods that were overridden
	 */
	public static List<Method> add(Object original, Object...objects) {
		if (Proxy.isProxyClass(original.getClass())) {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(original);
			while (invocationHandler instanceof CastInstanceInvoker) {
				original = ((CastInstanceInvoker) invocationHandler).getOriginal();
				if (!Proxy.isProxyClass(original.getClass())) {
					throw new IllegalArgumentException("The cast object is not an aspect");
				}
				invocationHandler = Proxy.getInvocationHandler(original);
			}
			if (invocationHandler instanceof AspectInstanceInvoker) {
				List<Method> methodsOfInterest = new ArrayList<Method>();
				for (Class<?> iface : getAllInterfaces(original.getClass())) {
					methodsOfInterest.addAll(Arrays.asList(iface.getMethods()));
				}
				List<Object> aspects = ((AspectInstanceInvoker) invocationHandler).getAspects();
				Map<Method, Object> methodMapping = ((AspectInstanceInvoker) invocationHandler).getMethodMapping();
				List<Method> overriddenMethods = new ArrayList<Method>();
				for (Object aspect : objects) {
					for (Class<?> iface : getAllInterfaces(aspect.getClass())) {
						for (Method method : iface.getMethods()) {
							if (methodsOfInterest.contains(method)) {
								try {
									Method implementation = getImplementation(aspect.getClass(), method);
									if (implementation.getAnnotation(NotImplemented.class) != null) {
										continue;
									}
								}
								catch (NoSuchMethodException e) {
									throw new RuntimeException(e);
								}
								methodMapping.put(method, aspect);
								aspects.add(aspect);
								overriddenMethods.add(method);
							}
						}
					}
				}
				return overriddenMethods;
			}
			else {
				throw new IllegalArgumentException("The object is a proxy but not an aspect");
			}
		}
		else {
			throw new IllegalArgumentException("The object is not an aspect");
		}
	}
	
	private static Method getImplementation(Class<?> clazz, Method method) throws NoSuchMethodException {
		while (clazz != null) {
			for (Method potential : clazz.getMethods()) {
				if (potential.getName().equals(method.getName()) && Arrays.equals(potential.getParameterTypes(), method.getParameterTypes())) {
					return potential;
				}
			}
			clazz = clazz.getSuperclass();
		}
		throw new NoSuchMethodException(method.toString());
	}
	
	/**
	 * This method can remove both by class and by aspect instance
	 * @return a list of methods that is now no longer implemented by the given object
	 */
	public static List<Method> remove(Object aspect, Object...objects) {
		if (Proxy.isProxyClass(aspect.getClass())) {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(aspect);
			while (invocationHandler instanceof CastInstanceInvoker) {
				aspect = ((CastInstanceInvoker) invocationHandler).getOriginal();
				if (!Proxy.isProxyClass(aspect.getClass())) {
					throw new IllegalArgumentException("The cast object is not an aspect");
				}
				invocationHandler = Proxy.getInvocationHandler(aspect);
			}
			if (invocationHandler instanceof AspectInstanceInvoker) {
				List<Object> objectList = Arrays.asList(objects);
				List<Object> aspects = ((AspectInstanceInvoker) invocationHandler).getAspects();
				List<Method> removedMethods = new ArrayList<Method>();
				Map<Method, Object> methodMapping = ((AspectInstanceInvoker) invocationHandler).getMethodMapping();
				Iterator<Entry<Method, Object>> iterator = methodMapping.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Method, Object> next = iterator.next();
					if (objectList.contains(next.getValue()) || objectList.contains(next.getValue().getClass())) {
						aspects.remove(next.getValue());
						removedMethods.add(next.getKey());
						iterator.remove();
					}
				}
				Iterator<Method> methodIterator = removedMethods.iterator();
				missing: while (methodIterator.hasNext()) {
					Method missingMethod = methodIterator.next();
					// try to fill the methods back in with the other objects
					for (int i = aspects.size() - 1; i >= 0; i--) {
						for (Class<?> iface : getAllInterfaces(aspects.get(i).getClass())) {
							List<Method> availableMethods = Arrays.asList(iface.getMethods());
							if (availableMethods.contains(missingMethod)) {
								methodMapping.put(missingMethod, aspects.get(i));
								methodIterator.remove();
								continue missing;
							}
						}
					}
				}
				return removedMethods;
			}
			else {
				throw new IllegalArgumentException("The object is a proxy but not an aspect");
			}
		}
		else {
			throw new IllegalArgumentException("The object is not an aspect");
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object instance, Class<T> iface) {
		if (iface.isAssignableFrom(instance.getClass())) {
			return (T) instance;
		}
		else {
			for (Method method : iface.getMethods()) {
				try {
					Method implementedMethod = getImplementation(instance.getClass(), method);
					if (!isCompatible(implementedMethod, method)) {
						throw new ClassCastException(instance.getClass().getName() + " is not compatible with " + iface.getName());
					}
				}
				catch (NoSuchMethodException e) {
					throw new ClassCastException(e.getMessage());
				}
				catch (SecurityException e) {
					throw new ClassCastException(e.getMessage());
				}
			}
			return (T) Proxy.newProxyInstance(instance.getClass().getClassLoader(), new Class[] { iface }, new CastInstanceInvoker(instance, iface));
		}
	}
	
	private static boolean isCompatible(Method implementedMethod, Method interfaceMethod) {
		if (!interfaceMethod.getReturnType().isAssignableFrom(implementedMethod.getReturnType())) {
			return false;
		}
		Class<?> [] expectedExceptionTypes = interfaceMethod.getExceptionTypes();
		// check exceptions thrown
		for (Class<?> exceptionType : implementedMethod.getExceptionTypes()) {
			boolean found = false;
			for (Class<?> expectedExceptionType : expectedExceptionTypes) {
				if (expectedExceptionType.isAssignableFrom(exceptionType)) {
					found = true;
					break;
				}
				if (!found)
					return false;
			}
		}
		// check parameters
		Class<?> [] expectedParameters = interfaceMethod.getParameterTypes();
		for (Class<?> parameter : implementedMethod.getParameterTypes()) {
			boolean found = false;
			for (Class<?> expectedParameter : expectedParameters) {
				if (parameter.isAssignableFrom(expectedParameter)) {
					found = true;
					break;
				}
				if (!found) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static class CastInstanceInvoker implements InvocationHandler {

		private Object original;
		private Class<?> iface;
		
		public CastInstanceInvoker(Object original, Class<?> iface) {
			this.original = original;
			this.iface = iface;
		}
		
		@Override
		public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
			return original.getClass().getMethod(arg1.getName(), arg1.getParameterTypes()).invoke(original, arg2);
		}

		public Object getOriginal() {
			return original;
		}

		public Class<?> getIface() {
			return iface;
		}
		
	}
	
	public static class AspectInstanceInvoker implements InvocationHandler {

		private Map<Method, Object> methodMapping;
		private List<Object> aspects;
		
		public AspectInstanceInvoker(Map<Method, Object> methodMapping, List<Object> aspects) {
			this.methodMapping = methodMapping;
			this.aspects = aspects;
		}
		
		@Override
		public Object invoke(Object target, Method method, Object[] arguments) throws Throwable {
			if (!methodMapping.containsKey(method)) {
				if (method.getDeclaringClass().equals(Object.class)) {
					return method.invoke(aspects.get(0), arguments);
				}
				throw new UnsupportedOperationException("The object does not support " + method);
			}
			else {
				return method.invoke(methodMapping.get(method), arguments);
			}
		}

		public Map<Method, Object> getMethodMapping() {
			return methodMapping;
		}

		public List<Object> getAspects() {
			return aspects;
		}
	}
}
