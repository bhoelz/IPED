package iped.app.ui;

import iped.data.IItemId;
import iped.datasource.IAdditionalDataSource;
import iped.engine.additionalindex.LuceneAdditionalDataSource;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.TaskInstallerConfig;
import iped.engine.data.IPEDSource;
import iped.engine.task.AbstractTask;
import iped.engine.task.additional.AdditionalTaskProgress;
import iped.engine.task.additional.AdditionalTaskRunner;
import iped.task.AdditionalProcessingCapable;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

/** Swing entry point for re-running an eligible task on selected case items. */
public final class AdditionalProcessingUI {

  private AdditionalProcessingUI() {}

  public static boolean hasEligibleTasks() {
    return !eligibleTasks().isEmpty();
  }

  public static void show(
      Component parent, IPEDSource source, Collection<? extends IItemId> selectedItems) {
    if (selectedItems == null || selectedItems.isEmpty()) {
      return;
    }
    List<TaskOption> tasks = eligibleTasks();
    if (tasks.isEmpty()) {
      JOptionPane.showMessageDialog(
          parent,
          "No additional-processing tasks are available.",
          "Additional processing",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    IAdditionalDataSource destination;
    try {
      destination = destination(source);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
          parent,
          "Could not open the additional index: " + e.getMessage(),
          "Additional processing",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    AdditionalProcessingDialog dialog =
        new AdditionalProcessingDialog(
            SwingUtilities.getWindowAncestor(parent),
            source,
            destination,
            new ArrayList<>(selectedItems),
            tasks);
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }

  private static IAdditionalDataSource destination(IPEDSource source) throws IOException {
    if (source.getAdditionalDataSourceManager().hasAnySources()) {
      return source.getAdditionalDataSourceManager().getSources().get(0);
    }
    Path path = source.getModuleDir().toPath().resolve(".additional-index");
    IAdditionalDataSource created = new LuceneAdditionalDataSource(path);
    source.getAdditionalDataSourceManager().register(created);
    return created;
  }

  private static List<TaskOption> eligibleTasks() {
    TaskInstallerConfig config = ConfigurationManager.get().findObject(TaskInstallerConfig.class);
    if (config == null) {
      return List.of();
    }
    List<TaskOption> result = new ArrayList<>();
    try {
      for (AbstractTask task : config.getNewTaskInstances()) {
        AdditionalProcessingCapable annotation =
            task.getClass().getAnnotation(AdditionalProcessingCapable.class);
        if (annotation != null) {
          result.add(
              new TaskOption(task.getClass(), annotation.displayName(), annotation.description()));
        }
      }
    } catch (RuntimeException ignored) {
      // A broken optional task must not prevent the UI from opening.
    }
    result.sort(Comparator.comparing(TaskOption::displayName));
    return result;
  }

  record TaskOption(
      Class<? extends AbstractTask> taskClass, String displayName, String description) {
    @Override
    public String toString() {
      return displayName;
    }
  }

  private static final class AdditionalProcessingDialog extends JDialog {
    private final IPEDSource source;
    private final IAdditionalDataSource destination;
    private final List<IItemId> selectedItems;
    private final List<TaskOption> tasks;
    private final JComboBox<TaskOption> taskBox;
    private final JSpinner threadSpinner;
    private final JCheckBox skipExisting;

    AdditionalProcessingDialog(
        Window owner,
        IPEDSource source,
        IAdditionalDataSource destination,
        List<IItemId> selectedItems,
        List<TaskOption> tasks) {
      super(owner, "Additional processing", ModalityType.APPLICATION_MODAL);
      this.source = source;
      this.destination = destination;
      this.selectedItems = selectedItems;
      this.tasks = tasks;
      this.taskBox = new JComboBox<>(tasks.toArray(TaskOption[]::new));
      this.threadSpinner =
          new JSpinner(
              new SpinnerNumberModel(
                  Math.max(1, Runtime.getRuntime().availableProcessors() / 2), 1, 64, 1));
      this.skipExisting = new JCheckBox("Skip items already processed by this task", true);
      build();
    }

    private void build() {
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      JPanel form = new JPanel(new GridBagLayout());
      form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(4, 4, 4, 4);
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      addRow(form, c, 0, "Task", taskBox);
      addRow(form, c, 1, "Threads", threadSpinner);
      c.gridx = 1;
      c.gridy = 2;
      form.add(skipExisting, c);

      JLabel description = new JLabel();
      taskBox.addActionListener(
          e -> description.setText(taskBox.getItemAt(taskBox.getSelectedIndex()).description()));
      description.setForeground(Color.GRAY);
      c.gridy = 3;
      form.add(description, c);
      if (!tasks.isEmpty()) description.setText(tasks.get(0).description());

      JButton cancel = new JButton("Cancel");
      JButton run = new JButton("Process " + selectedItems.size() + " item(s)");
      cancel.addActionListener(e -> dispose());
      run.addActionListener(e -> start());
      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttons.add(cancel);
      buttons.add(run);

      add(form, BorderLayout.CENTER);
      add(buttons, BorderLayout.SOUTH);
      pack();
      setMinimumSize(new Dimension(430, getPreferredSize().height));
    }

    private static void addRow(
        JPanel panel, GridBagConstraints c, int row, String label, Component component) {
      c.gridx = 0;
      c.gridy = row;
      c.weightx = 0;
      panel.add(new JLabel(label + ":"), c);
      c.gridx = 1;
      c.weightx = 1;
      panel.add(component, c);
    }

    private void start() {
      TaskOption option = (TaskOption) taskBox.getSelectedItem();
      if (option == null) return;
      List<IItemId> items =
          skipExisting.isSelected()
              ? selectedItems.stream().filter(id -> !hasResult(id, option)).toList()
              : selectedItems;
      if (items.isEmpty()) {
        JOptionPane.showMessageDialog(
            this, "All selected items already have a result for this task.");
        return;
      }
      AdditionalTaskProgressPanel panel = new AdditionalTaskProgressPanel(this, items.size());
      dispose();
      panel.start(
          new AdditionalTaskRunner(source, destination, (Integer) threadSpinner.getValue())
              .run(items, option.taskClass(), panel::update));
    }

    private boolean hasResult(IItemId id, TaskOption option) {
      return destination.hasTaskResult(id.getId(), option.taskClass().getSimpleName());
    }
  }

  private static final class AdditionalTaskProgressPanel extends JDialog {
    private final JProgressBar progress;
    private final JTextArea errors;
    private final JButton cancel;
    private CompletableFuture<AdditionalTaskProgress> future;

    AdditionalTaskProgressPanel(Window owner, int total) {
      super(owner, "Additional processing progress", ModalityType.MODELESS);
      progress = new JProgressBar(0, Math.max(1, total));
      progress.setStringPainted(true);
      errors = new JTextArea(5, 45);
      errors.setEditable(false);
      cancel = new JButton("Cancel");
      cancel.addActionListener(
          e -> {
            if (future != null) future.cancel(true);
            cancel.setEnabled(false);
          });
      JPanel content = new JPanel(new BorderLayout(8, 8));
      content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      content.add(progress, BorderLayout.NORTH);
      content.add(new JScrollPane(errors), BorderLayout.CENTER);
      content.add(cancel, BorderLayout.SOUTH);
      setContentPane(content);
      pack();
      setLocationRelativeTo(owner);
    }

    void start(CompletableFuture<AdditionalTaskProgress> future) {
      this.future = future;
      setVisible(true);
      future.whenComplete(
          (result, failure) ->
              SwingUtilities.invokeLater(
                  () -> {
                    cancel.setEnabled(false);
                    if (failure != null) errors.append(failure.getMessage() + "\n");
                    else if (result != null)
                      progress.setValue(result.getProcessed() + result.getErrors());
                  }));
    }

    void update(AdditionalTaskProgress update) {
      SwingUtilities.invokeLater(
          () -> {
            progress.setValue(update.getProcessed() + update.getErrors());
            progress.setString(
                update.getProcessed() + update.getErrors() + "/" + update.getTotal());
            if (update.getErrors() > 0) errors.append("Errors: " + update.getErrors() + "\n");
          });
    }
  }
}
