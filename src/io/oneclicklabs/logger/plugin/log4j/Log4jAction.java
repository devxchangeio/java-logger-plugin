
package io.oneclicklabs.logger.plugin.log4j;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

/**
 * @author karthy
 *
 * Sep 23, 2016
 */
public class Log4jAction implements IEditorActionDelegate {
	private static final String METHOD_ACTION = "oneclicklabs.log4j.method.action";
	private static final String LOG4J_ACTION = "oneclicklabs.log4j.action";
	private IEditorPart editor;
	private TextSelection text;

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.editor = targetEditor;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if ((selection instanceof TextSelection)) {
			this.text = ((TextSelection) selection);
		}
	}

	public void run(IAction action) {
		try {
			ICompilationUnit icu = (ICompilationUnit) JavaUI.getEditorInputJavaElement(this.editor.getEditorInput());

			Log4jUtility logger = new Log4jUtility(icu);
			if (action.getId().equals(METHOD_ACTION)) {
				logger.generateMethodLogging(this.text);
			} else if (action.getId().equals(LOG4J_ACTION)) {
				logger.generateSelectionLogging(this.text);
			}
		} catch (Exception e) {
			Shell shell = this.editor.getSite().getShell();
			MessageDialog.openError(shell, "Error",
					"An error occurred when trying to generate Log4j statements. Please check the Eclipse logs for more details.");
		}
	}
}
