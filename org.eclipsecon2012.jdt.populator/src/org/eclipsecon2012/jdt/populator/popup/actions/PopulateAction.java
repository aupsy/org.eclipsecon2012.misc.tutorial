package org.eclipsecon2012.jdt.populator.popup.actions;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipsecon2012.jdt.populator.Activator;

public class PopulateAction implements IObjectActionDelegate {

	private static final String[] PACKAGE_NAMES = {
		"p", "p", "foo", "foo", "bar"
	};
	private static final String[] CLASS_NAMES = {
		"P1", "TestAnnot", "C1",	"C2", 	"B"
	};
	private static final String[] CONTENTS = {
		"package p;\n" + 
		"\n" + 
		"public class P1 {\n" + 
		"	void foo(boolean b) {\n" + 
		"		Object o = goo();\n" + 
		"		if (b) {\n" + 
		"			o = null;\n" + 
		"		}\n" + 
		"		Object p = o;\n" + 
		"		p.toString();\n" + 
		"	}\n" + 
		"\n" + 
		"	Object goo() {\n" + 
		"		Object o = new Object();\n" + 
		"		int i = 1;\n" + 
		"		while (i <= 10) {\n" + 
		"			System.out.println(\"o at Iteration #\" + i + \" is-\" + o.toString());\n" + 
		"			if (i == 5) {\n" + 
		"				o = null;\n" + 
		"			} else {\n" + 
		"				o = new Integer(i);\n" + 
		"			}\n" + 
		"			i++;\n" + 
		"		}\n" + 
		"		return o;\n" + 
		"	}\n" + 
		"}\n",
		"package p;\n" + 
		"\n" + 
		"import org.eclipse.jdt.annotation.NonNull;\n" + 
		"import org.eclipse.jdt.annotation.Nullable;\n" + 
		"\n" + 
		"public class TestAnnot {\n" + 
		"	void foo(@Nullable Object o, @NonNull P1 p1){\n" + 
		"		// some code here\n" + 
		"		o.toString();\n" + 
		"		p1.foo(true);\n" + 
		"	}\n" + 
		"	public static void main(String[] args) {\n" + 
		"		new TestAnnot().foo(null, new P1());\n" + 
		"	}\n" + 
		"}\n",
		"package foo;\n" +
		"public class C1 {\n" +
		"    public String test(Object in, boolean f1, boolean f2) {\n" +
		"         if (f1) in = null;\n" +
		"         if (f2) {\n" +
		"             return in.toString();\n" +
		"         }\n" +
		"         return \"\";\n" +
		"    }\n" +
		"}\n",
		"package foo;\n" +
		"public class C2 extends C1 {\n" +
		"    @Override\n" +
		"    public String test(Object in, boolean f1, boolean f2) {\n" +
		"         return zork(in);\n" +
		"    }\n" +
		"    String zork(Object in) { return \"\"; }\n" +
		"}\n",
		"package bar;\n" +
		"import foo.*;\n" +
		"public class B {\n" +
		"    void unused1() { unused2(); }\n" +
		"    void unused2() { unused1(); }\n" +
		"    public static void main(String... args) {\n" +
		"         C1 c = new foo.C2();\n" +
		"         c.test(null, false, true);\n" +
		"         if (c == null)\n" +
		"              new B().unused1();\n" +
		"    }\n" +
		"}\n"
	};
	
	
	@SuppressWarnings("unused") // keep in case we want to add some message dialog
	private Shell shell;
	private IPackageFragmentRoot currentRoot;
	
	
	/**
	 * Constructor for Action1.
	 */
	public PopulateAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (this.currentRoot == null)
			return;
		new Job("Populate") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					populate(monitor);
				} catch (JavaModelException e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed", e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
		return;		
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection)selection).getFirstElement();
			if (first instanceof IPackageFragmentRoot) {
				this.currentRoot = (IPackageFragmentRoot)first;
			}
		}
	}

	private void populate(IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask("populating", PACKAGE_NAMES.length+1);
		for (int i=0; i<PACKAGE_NAMES.length; i++) {
			IPackageFragment fragment = this.currentRoot.createPackageFragment(PACKAGE_NAMES[i], true, monitor);
			fragment.createCompilationUnit(CLASS_NAMES[i]+".java", CONTENTS[i], true, monitor);
			monitor.worked(1);
		}
		configureProject((IJavaProject) this.currentRoot.getParent(), monitor);
		monitor.done();
	}

	private void configureProject(IJavaProject javaProject, IProgressMonitor monitor) 
			throws JavaModelException {
		javaProject.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		javaProject.setOption(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.ERROR);
		javaProject.setOption(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		addLibraryCPEntry(javaProject, 
						  "ECLIPSE_HOME",
						  "plugins/org.eclipse.jdt.annotation_1.0.0.v20120312-1601.jar",
						  monitor);
	}

	private void addLibraryCPEntry(IJavaProject javaProject, String variableName, 
			String relativeLibraryPath, IProgressMonitor monitor) 
					throws JavaModelException 
	{
		IPath eclipseHomePath = JavaCore.getClasspathVariable(variableName);
		IPath libraryPath = eclipseHomePath.append(relativeLibraryPath);
		IClasspathEntry annotationLibrary = JavaCore.newLibraryEntry(libraryPath, null, null);
		IClasspathEntry[] classpath = javaProject.getRawClasspath();
		int len = classpath.length;
		System.arraycopy(classpath, 0, classpath=new IClasspathEntry[len+1], 0, len);
		classpath[len] = annotationLibrary;
		javaProject.setRawClasspath(classpath, monitor);
	}

}
