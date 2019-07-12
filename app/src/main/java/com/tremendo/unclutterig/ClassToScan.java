package com.tremendo.unclutterig;

import java.lang.reflect.*;

import static de.robv.android.xposed.XposedHelpers.*;


public class ClassToScan {

	private Class<?> ClassToScan;


	public ClassToScan(Class<?> clazz) {
		this.ClassToScan = clazz;
	}



	public static ClassToScan find(String className, ClassLoader classLoader) {
		return new ClassToScan(className, classLoader);
	}



	private ClassToScan(String className, ClassLoader classLoader) {
		this.ClassToScan = findClass(className, classLoader);
	}



	public Method findMethodByName(String methodName) {
		for (Method method : this.ClassToScan.getDeclaredMethods()) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}



	public ClassLoader getClassLoader() {
		return this.ClassToScan.getClassLoader();
	}



	public String getName() {
		return this.ClassToScan.getName();
	}



	public boolean hasFieldType(Class<?> fieldType) {
		for (Field declaredField : this.ClassToScan.getDeclaredFields()) {
			Class<?> declaredFieldType = declaredField.getType();
			if (declaredFieldType == fieldType) {
				return true;
			}
		}
		return false;
	}



	public boolean hasMethodType(Class<?> methodReturnType) {
		for (Method declaredMethod : this.ClassToScan.getDeclaredMethods()) {
			if (methodReturnType == declaredMethod.getReturnType()) {
				return true;
			}
		}
		return false;
	}


}
