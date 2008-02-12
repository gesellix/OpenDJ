/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2007-2008 Sun Microsystems, Inc.
 */
package org.opends.server.util.cli;



import static org.opends.messages.UtilityMessages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opends.messages.Message;
import org.opends.server.util.table.TableBuilder;
import org.opends.server.util.table.TablePrinter;
import org.opends.server.util.table.TextTablePrinter;



/**
 * An interface for incrementally building a command-line menu.
 *
 * @param <T>
 *          The type of value returned by the call-backs. Use
 *          <code>Void</code> if the call-backs do not return a
 *          value.
 */
public final class MenuBuilder<T> {

  /**
   * A simple menu option call-back which is a composite of zero or
   * more underlying call-backs.
   *
   * @param <T>
   *          The type of value returned by the call-back.
   */
  private static final class CompositeCallback<T> implements MenuCallback<T> {

    // The list of underlying call-backs.
    private final Collection<MenuCallback<T>> callbacks;



    /**
     * Creates a new composite call-back with the specified set of
     * call-backs.
     *
     * @param callbacks
     *          The set of call-backs.
     */
    public CompositeCallback(Collection<MenuCallback<T>> callbacks) {
      this.callbacks = callbacks;
    }



    /**
     * {@inheritDoc}
     */
    public MenuResult<T> invoke(ConsoleApplication app) throws CLIException {
      List<T> values = new ArrayList<T>();
      for (MenuCallback<T> callback : callbacks) {
        MenuResult<T> result = callback.invoke(app);

        if (!result.isSuccess()) {
          // Throw away all the other results.
          return result;
        } else {
          values.addAll(result.getValues());
        }
      }
      return MenuResult.success(values);
    }
  }



  /**
   * Underlying menu implementation generated by this menu builder.
   *
   * @param <T>
   *          The type of value returned by the call-backs. Use
   *          <code>Void</code> if the call-backs do not return a
   *          value.
   */
  private static final class MenuImpl<T> implements Menu<T> {

    // Indicates whether the menu will allow selection of multiple
    // numeric options.
    private final boolean allowMultiSelect;

    // The application console.
    private final ConsoleApplication app;

    // The call-back lookup table.
    private final Map<String, MenuCallback<T>> callbacks;

    // The char options table builder.
    private final TableBuilder cbuilder;

    // The call-back for the optional default action.
    private final MenuCallback<T> defaultCallback;

    // The description of the optional default action.
    private final Message defaultDescription;

    // The numeric options table builder.
    private final TableBuilder nbuilder;

    // The table printer.
    private final TablePrinter printer;

    // The menu prompt.
    private final Message prompt;

    // The menu title.
    private final Message title;



    // Private constructor.
    private MenuImpl(ConsoleApplication app, Message title, Message prompt,
        TableBuilder ntable, TableBuilder ctable, TablePrinter printer,
        Map<String, MenuCallback<T>> callbacks, boolean allowMultiSelect,
        MenuCallback<T> defaultCallback, Message defaultDescription) {
      this.app = app;
      this.title = title;
      this.prompt = prompt;
      this.nbuilder = ntable;
      this.cbuilder = ctable;
      this.printer = printer;
      this.callbacks = callbacks;
      this.allowMultiSelect = allowMultiSelect;
      this.defaultCallback = defaultCallback;
      this.defaultDescription = defaultDescription;
    }



    /**
     * {@inheritDoc}
     */
    public MenuResult<T> run() throws CLIException {
      // The validation call-back which will be used to determine the
      // action call-back.
      ValidationCallback<MenuCallback<T>> validator =
        new ValidationCallback<MenuCallback<T>>() {

        public MenuCallback<T> validate(ConsoleApplication app, String input) {
          String ninput = input.trim();

          if (ninput.length() == 0) {
            if (defaultCallback != null) {
              return defaultCallback;
            } else if (allowMultiSelect) {
              app.println();
              app.println(ERR_MENU_BAD_CHOICE_MULTI.get());
              app.println();
              return null;
            } else {
              app.println();
              app.println(ERR_MENU_BAD_CHOICE_SINGLE.get());
              app.println();
              return null;
            }
          } else if (allowMultiSelect) {
            // Use a composite call-back to collect all the results.
            List<MenuCallback<T>> cl = new ArrayList<MenuCallback<T>>();
            for (String value : ninput.split(",")) {
              // Make sure that there are no duplicates.
              String nvalue = value.trim();
              Set<String> choices = new HashSet<String>();

              if (choices.contains(nvalue)) {
                app.println();
                app.println(ERR_MENU_BAD_CHOICE_MULTI_DUPE.get(value));
                app.println();
                return null;
              } else if (!callbacks.containsKey(nvalue)) {
                app.println();
                app.println(ERR_MENU_BAD_CHOICE_MULTI.get());
                app.println();
                return null;
              } else {
                cl.add(callbacks.get(nvalue));
                choices.add(nvalue);
              }
            }

            return new CompositeCallback<T>(cl);
          } else if (!callbacks.containsKey(ninput)) {
            app.println();
            app.println(ERR_MENU_BAD_CHOICE_SINGLE.get());
            app.println();
            return null;
          } else {
            return callbacks.get(ninput);
          }
        }
      };

      // Determine the correct choice prompt.
      Message promptMsg;
      if (allowMultiSelect) {
        if (defaultDescription != null) {
          promptMsg = INFO_MENU_PROMPT_MULTI_DEFAULT.get(defaultDescription);
        } else {
          promptMsg = INFO_MENU_PROMPT_MULTI.get();
        }
      } else {
        if (defaultDescription != null) {
          promptMsg = INFO_MENU_PROMPT_SINGLE_DEFAULT.get(defaultDescription);
        } else {
          promptMsg = INFO_MENU_PROMPT_SINGLE.get();
        }
      }

      // If the user selects help then we need to loop around and
      // display the menu again.
      while (true) {
        // Display the menu.
        if (title != null) {
          app.println(title);
          app.println();
        }

        if (prompt != null) {
          app.println(prompt);
          app.println();
        }

        if (nbuilder.getTableHeight() > 0) {
          nbuilder.print(printer);
          app.println();
        }

        if (cbuilder.getTableHeight() > 0) {
          TextTablePrinter cprinter =
            new TextTablePrinter(app.getErrorStream());
          cprinter.setDisplayHeadings(false);
          int sz = String.valueOf(nbuilder.getTableHeight()).length() + 1;
          cprinter.setIndentWidth(4);
          cprinter.setColumnWidth(0, sz);
          cprinter.setColumnWidth(1, 0);
          cbuilder.print(cprinter);
          app.println();
        }

        // Get the user's choice.
        MenuCallback<T> choice = app.readValidatedInput(promptMsg, validator);

        // Invoke the user's selected choice.
        MenuResult<T> result = choice.invoke(app);

        // Determine if the help needs to be displayed, display it and
        // start again.
        if (!result.isAgain()) {
          return result;
        } else {
          app.println();
          app.println();
        }
      }
    }
  }



  /**
   * A simple menu option call-back which does nothing but return the
   * provided menu result.
   *
   * @param <T>
   *          The type of result returned by the call-back.
   */
  private static final class ResultCallback<T> implements MenuCallback<T> {

    // The result to be returned by this call-back.
    private final MenuResult<T> result;



    // Private constructor.
    private ResultCallback(MenuResult<T> result) {
      this.result = result;
    }



    /**
     * {@inheritDoc}
     */
    public MenuResult<T> invoke(ConsoleApplication app) throws CLIException {
      return result;
    }

  }

  // The multiple column display threshold.
  private int threshold = -1;

  // Indicates whether the menu will allow selection of multiple
  // numeric options.
  private boolean allowMultiSelect = false;

  // The application console.
  private final ConsoleApplication app;

  // The char option call-backs.
  private final List<MenuCallback<T>> charCallbacks =
    new ArrayList<MenuCallback<T>>();

  // The char option keys (must be single-character messages).
  private final List<Message> charKeys = new ArrayList<Message>();

  // The synopsis of char options.
  private final List<Message> charSynopsis = new ArrayList<Message>();

  // Optional column headings.
  private final List<Message> columnHeadings = new ArrayList<Message>();

  // Optional column widths.
  private final List<Integer> columnWidths = new ArrayList<Integer>();

  // The call-back for the optional default action.
  private MenuCallback<T> defaultCallback = null;

  // The description of the optional default action.
  private Message defaultDescription = null;

  // The numeric option call-backs.
  private final List<MenuCallback<T>> numericCallbacks =
    new ArrayList<MenuCallback<T>>();

  // The numeric option fields.
  private final List<List<Message>> numericFields =
    new ArrayList<List<Message>>();

  // The menu title.
  private Message title = null;

  // The menu prompt.
  private Message prompt = null;



  /**
   * Creates a new menu.
   *
   * @param app
   *          The application console.
   */
  public MenuBuilder(ConsoleApplication app) {
    this.app = app;
  }



  /**
   * Creates a "back" menu option. When invoked, this option will
   * return a {@code MenuResult.cancel()} result.
   *
   * @param isDefault
   *          Indicates whether this option should be made the menu
   *          default.
   */
  public void addBackOption(boolean isDefault) {
    addCharOption(INFO_MENU_OPTION_BACK_KEY.get(), INFO_MENU_OPTION_BACK.get(),
        MenuResult.<T> cancel());

    if (isDefault) {
      setDefault(INFO_MENU_OPTION_BACK_KEY.get(), MenuResult.<T> cancel());
    }
  }



  /**
   * Creates a "cancel" menu option. When invoked, this option will
   * return a {@code MenuResult.cancel()} result.
   *
   * @param isDefault
   *          Indicates whether this option should be made the menu
   *          default.
   */
  public void addCancelOption(boolean isDefault) {
    addCharOption(INFO_MENU_OPTION_CANCEL_KEY.get(), INFO_MENU_OPTION_CANCEL
        .get(), MenuResult.<T> cancel());

    if (isDefault) {
      setDefault(INFO_MENU_OPTION_CANCEL_KEY.get(), MenuResult.<T> cancel());
    }
  }



  /**
   * Adds a menu choice to the menu which will have a single letter as
   * its key.
   *
   * @param c
   *          The single-letter message which will be used as the key
   *          for this option.
   * @param description
   *          The menu option description.
   * @param callback
   *          The call-back associated with this option.
   */
  public void addCharOption(Message c, Message description,
      MenuCallback<T> callback) {
    charKeys.add(c);
    charSynopsis.add(description);
    charCallbacks.add(callback);
  }



  /**
   * Adds a menu choice to the menu which will have a single letter as
   * its key and which returns the provided result.
   *
   * @param c
   *          The single-letter message which will be used as the key
   *          for this option.
   * @param description
   *          The menu option description.
   * @param result
   *          The menu result which should be returned by this menu
   *          choice.
   */
  public void addCharOption(Message c, Message description,
      MenuResult<T> result) {
    addCharOption(c, description, new ResultCallback<T>(result));
  }



  /**
   * Creates a "help" menu option which will use the provided help
   * call-back to display help relating to the other menu options.
   * When the help menu option is selected help will be displayed and
   * then the user will be shown the menu again and prompted to enter
   * a choice.
   *
   * @param callback
   *          The help call-back.
   */
  public void addHelpOption(final HelpCallback callback) {
    MenuCallback<T> wrapper = new MenuCallback<T>() {

      public MenuResult<T> invoke(ConsoleApplication app) throws CLIException {
        app.println();
        callback.display(app);
        return MenuResult.again();
      }

    };

    addCharOption(INFO_MENU_OPTION_HELP_KEY.get(), INFO_MENU_OPTION_HELP.get(),
        wrapper);
  }



  /**
   * Adds a menu choice to the menu which will have a numeric key.
   *
   * @param description
   *          The menu option description.
   * @param callback
   *          The call-back associated with this option.
   * @param extraFields
   *          Any additional fields associated with this menu option.
   * @return Returns the number associated with menu choice.
   */
  public int addNumberedOption(Message description, MenuCallback<T> callback,
      Message... extraFields) {
    List<Message> fields = new ArrayList<Message>();
    fields.add(description);
    if (extraFields != null) {
      fields.addAll(Arrays.asList(extraFields));
    }

    numericFields.add(fields);
    numericCallbacks.add(callback);

    return numericCallbacks.size();
  }



  /**
   * Adds a menu choice to the menu which will have a numeric key and
   * which returns the provided result.
   *
   * @param description
   *          The menu option description.
   * @param result
   *          The menu result which should be returned by this menu
   *          choice.
   * @param extraFields
   *          Any additional fields associated with this menu option.
   * @return Returns the number associated with menu choice.
   */
  public int addNumberedOption(Message description, MenuResult<T> result,
      Message... extraFields) {
    return addNumberedOption(description, new ResultCallback<T>(result),
        extraFields);
  }



  /**
   * Creates a "quit" menu option. When invoked, this option will
   * return a {@code MenuResult.quit()} result.
   */
  public void addQuitOption() {
    addCharOption(INFO_MENU_OPTION_QUIT_KEY.get(), INFO_MENU_OPTION_QUIT.get(),
        MenuResult.<T> quit());
  }



  /**
   * Sets the flag which indicates whether or not the menu will permit
   * multiple numeric options to be selected at once. Users specify
   * multiple choices by separating them with a comma. The default is
   * <code>false</code>.
   *
   * @param allowMultiSelect
   *          Indicates whether or not the menu will permit multiple
   *          numeric options to be selected at once.
   */
  public void setAllowMultiSelect(boolean allowMultiSelect) {
    this.allowMultiSelect = allowMultiSelect;
  }



  /**
   * Sets the optional column headings. The column headings will be
   * displayed above the menu options.
   *
   * @param headings
   *          The optional column headings.
   */
  public void setColumnHeadings(Message... headings) {
    this.columnHeadings.clear();
    if (headings != null) {
      this.columnHeadings.addAll(Arrays.asList(headings));
    }
  }



  /**
   * Sets the optional column widths. A value of zero indicates that
   * the column should be expandable, a value of <code>null</code>
   * indicates that the column should use its default width.
   *
   * @param widths
   *          The optional column widths.
   */
  public void setColumnWidths(Integer... widths) {
    this.columnWidths.clear();
    if (widths != null) {
      this.columnWidths.addAll(Arrays.asList(widths));
    }
  }



  /**
   * Sets the optional default action for this menu. The default
   * action call-back will be invoked if the user does not specify an
   * option and just presses enter.
   *
   * @param description
   *          A short description of the default action.
   * @param callback
   *          The call-back associated with the default action.
   */
  public void setDefault(Message description, MenuCallback<T> callback) {
    defaultCallback = callback;
    defaultDescription = description;
  }



  /**
   * Sets the optional default action for this menu. The default
   * action call-back will be invoked if the user does not specify an
   * option and just presses enter.
   *
   * @param description
   *          A short description of the default action.
   * @param result
   *          The menu result which should be returned by default.
   */
  public void setDefault(Message description, MenuResult<T> result) {
    setDefault(description, new ResultCallback<T>(result));
  }



  /**
   * Sets the number of numeric options required to trigger
   * multiple-column display. A negative value (the default) indicates
   * that the numeric options will always be displayed in a single
   * column. A value of 0 indicates that numeric options will always
   * be displayed in multiple columns.
   *
   * @param threshold
   *          The number of numeric options required to trigger
   *          multiple-column display.
   */
  public void setMultipleColumnThreshold(int threshold) {
    this.threshold = threshold;
  }



  /**
   * Sets the optional menu prompt. The prompt will be displayed above
   * the menu. Menus do not have a prompt by default.
   *
   * @param prompt
   *          The menu prompt, or <code>null</code> if there is not
   *          prompt.
   */
  public void setPrompt(Message prompt) {
    this.prompt = prompt;
  }



  /**
   * Sets the optional menu title. The title will be displayed above
   * the menu prompt. Menus do not have a title by default.
   *
   * @param title
   *          The menu title, or <code>null</code> if there is not
   *          title.
   */
  public void setTitle(Message title) {
    this.title = title;
  }



  /**
   * Creates a menu from this menu builder.
   *
   * @return Returns the new menu.
   */
  public Menu<T> toMenu() {
    TableBuilder nbuilder = new TableBuilder();
    Map<String, MenuCallback<T>> callbacks =
      new HashMap<String, MenuCallback<T>>();

    // Determine whether multiple columns should be used for numeric
    // options.
    boolean useMultipleColumns = false;
    if (threshold >= 0 && numericCallbacks.size() >= threshold) {
      useMultipleColumns = true;
    }

    // Create optional column headers.
    if (!columnHeadings.isEmpty()) {
      nbuilder.appendHeading();
      for (Message heading : columnHeadings) {
        if (heading != null) {
          nbuilder.appendHeading(heading);
        } else {
          nbuilder.appendHeading();
        }
      }

      if (useMultipleColumns) {
        nbuilder.appendHeading();
        for (Message heading : columnHeadings) {
          if (heading != null) {
            nbuilder.appendHeading(heading);
          } else {
            nbuilder.appendHeading();
          }
        }
      }
    }

    // Add the numeric options first.
    int sz = numericCallbacks.size();
    int rows = sz;

    if (useMultipleColumns) {
      // Display in two columns the first column should contain half
      // the options. If there are an odd number of columns then the
      // first column should contain an additional option (e.g. if
      // there are 23 options, the first column should contain 12
      // options and the second column 11 options).
      rows /= 2;
      rows += sz % 2;
    }

    for (int i = 0, j = rows; i < rows; i++, j++) {
      nbuilder.startRow();
      nbuilder.appendCell(INFO_MENU_NUMERIC_OPTION.get(i + 1));

      for (Message field : numericFields.get(i)) {
        if (field != null) {
          nbuilder.appendCell(field);
        } else {
          nbuilder.appendCell();
        }
      }

      callbacks.put(String.valueOf(i + 1), numericCallbacks.get(i));

      // Second column.
      if (useMultipleColumns && (j < sz)) {
        nbuilder.appendCell(INFO_MENU_NUMERIC_OPTION.get(j + 1));

        for (Message field : numericFields.get(j)) {
          if (field != null) {
            nbuilder.appendCell(field);
          } else {
            nbuilder.appendCell();
          }
        }

        callbacks.put(String.valueOf(j + 1), numericCallbacks.get(j));
      }
    }

    // Add the char options last.
    TableBuilder cbuilder = new TableBuilder();
    for (int i = 0; i < charCallbacks.size(); i++) {
      char c = charKeys.get(i).charAt(0);
      Message option = INFO_MENU_CHAR_OPTION.get(c);

      cbuilder.startRow();
      cbuilder.appendCell(option);
      cbuilder.appendCell(charSynopsis.get(i));

      callbacks.put(String.valueOf(c), charCallbacks.get(i));
    }

    // Configure the table printer.
    TextTablePrinter printer = new TextTablePrinter(app.getErrorStream());

    if (columnHeadings.isEmpty()) {
      printer.setDisplayHeadings(false);
    } else {
      printer.setDisplayHeadings(true);
      printer.setHeadingSeparatorStartColumn(1);
    }

    printer.setIndentWidth(4);
    if (columnWidths.isEmpty()) {
      printer.setColumnWidth(1, 0);
      if (useMultipleColumns) {
        printer.setColumnWidth(3, 0);
      }
    } else {
      for (int i = 0; i < columnWidths.size(); i++) {
        Integer j = columnWidths.get(i);
        if (j != null) {
          // Skip the option key column.
          printer.setColumnWidth(i + 1, j);

          if (useMultipleColumns) {
            printer.setColumnWidth(i + 2 + columnWidths.size(), j);
          }
        }
      }
    }

    return new MenuImpl<T>(app, title, prompt, nbuilder, cbuilder, printer,
        callbacks, allowMultiSelect, defaultCallback, defaultDescription);
  }
}
