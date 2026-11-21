package coderev;

import coderev.ui.CodeRevApp;

/**
 * Entry point.
 *   java coderev.Main            launches the JavaFX interface
 *   java coderev.Main --console  launches the text-mode interface
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            if ("--console".equals(arg)) {
                coderev.console.ConsoleApp.main(args);
                return;
            }
        }
        CodeRevApp.launch(CodeRevApp.class, args);
    }
}
