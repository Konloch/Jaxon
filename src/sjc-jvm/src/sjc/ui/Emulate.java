/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Stefan Frenz and Patrick Schmidt
 *
 * This file is part of SJC, the Small Java Compiler written by Stefan Frenz.
 *
 * SJC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SJC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SJC. If not, see <http://www.gnu.org/licenses/>.
 */

package sjc.ui;

import sjc.emulation.*;
import sjc.emulation.cond.BreakPoint;
import sjc.emulation.cond.MemoryBreak;
import sjc.osio.TextPrinter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Emulate: main window and main user interface for emulation with JavaGUI
 *
 * @author S. Frenz, P. Schmidt
 * @version 100412 made use of generic utility classes of jdk1.5
 * version 100206 optimized drawChar
 * version 091005 changed displayed title to "ssaEmulator"
 * version 090303 adopted changed osio package sjc.structure and integrated GUIConsole
 * version 090207 added copyright notice
 * version 080704 added serialVersionUID
 * version 070817 made creation of PrintWriter compatible to jdk1.4.2
 * version 070501 added some checks catching faulty backends/architectures
 * version 060607 initial version
 */

public class Emulate extends JFrame implements ActionListener, MouseListener, KeyListener, BreakPointListener
{
	
	/**
	 * Default dimension of the buttons
	 */
	private static final Dimension BUTTONS = new Dimension(80, 20);
	
	private static final int MODE_ERROR = 0;
	private static final int MODE_START = 1;
	private static final int MODE_STATISTIC = 2;
	private static final int MODE_STOP = 3;
	
	/**
	 * Abstract superclass of dialogs
	 *
	 * @author Patrick Schmidt
	 * @version 060622 initial version
	 */
	private abstract class Dialogues extends JDialog implements ActionListener
	{
		
		public static final long serialVersionUID = 16047801001l;
		
		/**
		 * Standard construction setting parent frame and title
		 *
		 * @param parent the parent container
		 * @param title  the title of this dialog
		 */
		public Dialogues(Frame parent, String title)
		{
			super(parent, title, true);
			//      init();
		}
		
		/**
		 * Standard construction setting parent dialog and title
		 *
		 * @param parent the parent container
		 * @param title  the title of this dialog
		 */
		public Dialogues(JDialog parent, String title)
		{
			super(parent, title, true);
			//      init();
		}
		
		/**
		 * Method to initializing the GUI, such as widgets, size,...
		 */
		protected abstract void init();
		
		/**
		 * Method which is performed each time this window is set visible
		 */
		protected abstract void setVisible();
		
		/**
		 * Method called whenever a button is pressed
		 *
		 * @param actionCommand the action command
		 */
		protected abstract void processButton(String actionCommand);
		
		/**
		 * @see ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e)
		{
			processButton(e.getActionCommand());
		}
		
		/**
		 * @see Component#setVisible(boolean)
		 */
		public void setVisible(boolean v)
		{
			if (v)
			{
				Container parent = getParent();
				setLocation(parent.getX() + (parent.getWidth() - getWidth()) / 2, parent.getY() + (parent.getHeight() - getHeight()) / 2);
				setVisible();
			}
			super.setVisible(v);
		}
	}
	
	/**
	 * Dialog for displaying the result of the statistic
	 *
	 * @author Patrick Schmidt
	 * @version 060621 initial version
	 */
	private class Statistics extends Dialogues implements MouseListener
	{
		
		/**
		 * Cell renderer for the alignment of the cells
		 *
		 * @author Patrick Schmidt
		 * @version 060622 initial version
		 */
		private class Renderer extends DefaultTableCellRenderer
		{
			
			public static final long serialVersionUID = 16047801001l;
			
			/**
			 * Standard constructor
			 */
			public Renderer()
			{
				super();
				setHorizontalAlignment(SwingConstants.RIGHT);
			}
		}
		
		private abstract class StatisticsComparator implements Comparator<MethodStatistics>
		{}
		
		/**
		 * Comparator to sort a Collection by the number of calls
		 *
		 * @author Patrick Schmidt
		 * @version 060622 initial version
		 */
		private class CallsComparator extends StatisticsComparator
		{
			
			/**
			 * @see Comparator#compare(T, T)
			 */
			public int compare(MethodStatistics arg0, MethodStatistics arg1)
			{
				int res = (int) (arg1.numberOfCalls - arg0.numberOfCalls);
				if (res == 0)
					res = (int) (arg1.numberOfExecIns - arg0.numberOfExecIns);
				if (res == 0)
					return arg0.mthPtr - arg1.mthPtr;
				return res;
			}
		}
		
		/**
		 * Comparator to sort a Collection by the number of executed
		 * instructions
		 *
		 * @author Patrick Schmidt
		 * @version 060622 initial version
		 */
		private class NumberOfInsComparator extends StatisticsComparator
		{
			
			/**
			 * @see Comparator#compare(T, T)
			 */
			public int compare(MethodStatistics arg0, MethodStatistics arg1)
			{
				int res = (int) (arg1.numberOfExecIns - arg0.numberOfExecIns);
				if (res == 0)
					res = (int) (arg1.numberOfCalls - arg0.numberOfCalls);
				if (res == 0)
					return arg0.mthPtr - arg1.mthPtr;
				return res;
			}
		}
		
		/**
		 * Comparator to sort a Collection by the method pointers
		 *
		 * @author Patrick Schmidt
		 * @version 060622 initial version
		 */
		private class MthPtrComparator extends StatisticsComparator
		{
			
			/**
			 * @see Comparator#compare(T, T)
			 */
			public int compare(MethodStatistics arg0, MethodStatistics arg1)
			{
				return arg0.mthPtr - arg1.mthPtr;
			}
		}
		
		/**
		 * Class wrapping a TreeSet containing the method statistics into
		 * a TableModel
		 *
		 * @author Patrick Schmidt
		 * @version 060622 initial version
		 */
		private class StatisticsTableData implements TableModel
		{
			
			/**
			 * Constant for sorting by method pointer
			 */
			private static final int MTH_PTR = 0;
			
			/**
			 * Constant for sorting by number of calls
			 */
			private static final int CALL = 1;
			
			/**
			 * Constant for sorting by number of instructions
			 */
			private static final int INS = 2;
			
			/**
			 * Array holding the available comparators
			 */
			private final ArrayList<StatisticsComparator> comps;
			
			/**
			 * Treeset containing the data
			 */
			private TreeSet<MethodStatistics> mths;
			
			/**
			 * The current sorting mode of this TableModel
			 */
			private int curMode;
			
			/**
			 * Standard constructor
			 */
			public StatisticsTableData()
			{
				curMode = -1;
				comps = new ArrayList<StatisticsComparator>();
				comps.add(MTH_PTR, new MthPtrComparator());
				comps.add(CALL, new CallsComparator());
				comps.add(INS, new NumberOfInsComparator());
				mths = new TreeSet<MethodStatistics>(comps.get(MTH_PTR));
				insertMths(methStatMain);
			}
			
			/**
			 * Method inserting the method statistics in the given TreeSet
			 *
			 * @param mth the method to insert
			 */
			private void insertMths(MethodStatistics mth)
			{
				if (mth == null || mths == null)
					return;
				mths.add(mth);
				insertMths(mth.left);
				insertMths(mth.right);
			}
			
			public void addTableModelListener(TableModelListener l)
			{
				// nothing to do
			}
			
			public Class<MethodStatistics> getColumnClass(int columnIndex)
			{
				return MethodStatistics.class;
			}
			
			public int getColumnCount()
			{
				return 3;
			}
			
			public String getColumnName(int columnIndex)
			{
				switch (columnIndex)
				{
					case 0:
						return "Method pointer";
					case 1:
						return "#calls";
					case 2:
						return "#instructions";
				}
				return null;
			}
			
			public int getRowCount()
			{
				if (mths == null)
					return 0;
				return mths.size();
			}
			
			public Object getValueAt(int rowIndex, int columnIndex)
			{
				if (mths == null)
					return null;
				Iterator<MethodStatistics> it = mths.iterator();
				MethodStatistics res = null;
				int cnt = 0;
				while (it.hasNext())
				{
					if (cnt == rowIndex)
					{
						res = it.next();
						break;
					}
					it.next();
					cnt++;
				}
				if (res != null)
				{
					switch (columnIndex)
					{
						case 0:
							return Integer.toHexString(res.mthPtr);
						case 1:
							return Long.toString(res.numberOfCalls);
						case 2:
							return Long.toString(res.numberOfExecIns);
					}
				}
				return null;
			}
			
			public boolean isCellEditable(int rowIndex, int columnIndex)
			{
				return false;
			}
			
			public void removeTableModelListener(TableModelListener l)
			{
				// nothing to do
			}
			
			public void setValueAt(Object aValue, int rowIndex, int columnIndex)
			{
				// nothing to do
			}
			
			/**
			 * Method setting the sorting mode by which the JTable is sorted
			 *
			 * @param mode the mode to sort by
			 */
			public void setMode(int mode)
			{
				if (mode != curMode)
				{
					curMode = mode;
					mths = new TreeSet<MethodStatistics>(comps.get(curMode));
					insertMths(methStatMain);
					data.tableChanged(null);
				}
			}
			
		}
		
		
		public static final long serialVersionUID = 16047801001l;
		
		/**
		 * Label and action command for the close button
		 */
		private static final String CLOSE_B = "close";
		
		/**
		 * Label and action command for the export button
		 */
		private static final String EXPORT_B = "export";
		
		/**
		 * Label and action command for the reset button
		 */
		private static final String RESET_B = "reset";
		
		/**
		 * Textarea containing the statistics
		 */
		private JTable data;
		
		/**
		 * Tablemodel containing the statistics
		 */
		private StatisticsTableData dataModel;
		
		/**
		 * RadioButton for sorting by method pointer
		 */
		private JRadioButton methodSort;
		
		/**
		 * RadioButton for sorting by number of calls
		 */
		private JRadioButton callSort;
		
		/**
		 * RadioButton for sorting by number of executed instructions
		 */
		private JRadioButton insSort;
		
		/**
		 * JLabel containing the number of executed instructions
		 */
		private JLabel nrOfIns;
		
		/**
		 * Standard constructor
		 *
		 * @param parent the parent container
		 */
		public Statistics(Frame parent)
		{
			super(parent, "Statistics for emulation");
		}
		
		/**
		 * @see Dialogues#init()
		 */
		protected void init()
		{
			setLayout(new BorderLayout());
			
			// create JTable with scrollpane and add it to the frame
			dataModel = new StatisticsTableData();
			data = new JTable(dataModel);
			data.setDefaultRenderer(dataModel.getColumnClass(0), new Renderer());
			add(new JScrollPane(data), BorderLayout.CENTER);
			
			// Radiogroup for sorting
			JPanel radio = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
			radio.setBorder(new TitledBorder("Sort by"));
			// Radiobuttons
			methodSort = new JRadioButton("Method pointer", true);
			methodSort.addMouseListener(this);
			callSort = new JRadioButton("#calls");
			callSort.addMouseListener(this);
			insSort = new JRadioButton("#instructions");
			insSort.addMouseListener(this);
			// add to the Panel
			radio.add(methodSort);
			radio.add(callSort);
			radio.add(insSort);
			// Buttongroup for exclusive selection
			ButtonGroup group = new ButtonGroup();
			group.add(methodSort);
			group.add(callSort);
			group.add(insSort);
			
			// panel containing the buttons in the south section
			JPanel temp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			temp.add(createButton(EXPORT_B, this, BUTTONS));
			temp.add(createButton(RESET_B, this, BUTTONS));
			temp.add(createButton(CLOSE_B, this, BUTTONS));
			
			// south section containing the radiogroup and the buttons
			JPanel south = new JPanel(new BorderLayout());
			south.add(radio, BorderLayout.CENTER);
			south.add(temp, BorderLayout.SOUTH);
			add(south, BorderLayout.SOUTH);
			
			nrOfIns = new JLabel();
			add(nrOfIns, BorderLayout.NORTH);
			
			setSize(325, 450);
		}
		
		/**
		 * @see Dialogues#processButton(String)
		 */
		protected void processButton(String actionCommand)
		{
			if (actionCommand.equals(CLOSE_B))
				setVisible(false);
			else if (actionCommand.equals(EXPORT_B))
			{
				// create String containing the text to transfer
				String result = "";
				Iterator<MethodStatistics> it = dataModel.mths.iterator();
				MethodStatistics ms;
				while (it.hasNext())
				{
					ms = it.next();
					result += "0x" + Integer.toHexString(ms.mthPtr) + "\t" + ms.numberOfCalls + "\t" + ms.numberOfExecIns + "\n";
				}
				// get the clipboard from the Toolkit, create a
				// StringSelection and set the clipboard
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
				JOptionPane.showMessageDialog(this, "Data was copied to the " + "clipboard", "Information", JOptionPane.INFORMATION_MESSAGE);
			}
			else if (actionCommand.equals(RESET_B))
			{
				methStatMain.reset();
				numberOfExecIns = 0;
				nrOfIns.setText("");
				data.tableChanged(null);
			}
		}
		
		/**
		 * @see Dialogues#setVisible()
		 */
		protected void setVisible()
		{
			nrOfIns.setText("executed instructions: " + numberOfExecIns);
			methodSort.setSelected(true);
			dataModel.setMode(StatisticsTableData.MTH_PTR);
		}
		
		/**
		 * @see MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		public void mouseClicked(MouseEvent e)
		{
			if (methodSort.isSelected())
				dataModel.setMode(StatisticsTableData.MTH_PTR);
			else if (callSort.isSelected())
				dataModel.setMode(StatisticsTableData.CALL);
			else if (insSort.isSelected())
				dataModel.setMode(StatisticsTableData.INS);
		}
		
		/**
		 * @see MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		public void mouseEntered(MouseEvent e)
		{
		}
		
		/**
		 * @see MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		public void mouseExited(MouseEvent e)
		{
		}
		
		/**
		 * @see MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public void mousePressed(MouseEvent e)
		{
		}
		
		/**
		 * @see MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent e)
		{
		}
	}
	
	/**
	 * File filter for the file chooser
	 *
	 * @author Patrick Schmidt
	 */
	private class Filter extends FileFilter
	{
		
		/**
		 * String for the description
		 */
		private final String descr;
		
		/**
		 * String for the start of file name
		 */
		private final String name;
		
		/**
		 * String for the file extension
		 */
		private final String ext;
		
		/**
		 * Flag to determine whether either the name or the extension or both has to
		 * be checked
		 */
		private final boolean both;
		
		/**
		 * Standard constructor
		 *
		 * @param descr the description of this filter
		 * @param name  the name of the file which is to be displayed
		 * @param ext   the extension of the files which are to be displayed
		 * @param both  flag to determine whether both name and ext are to be checked
		 */
		public Filter(String descr, String name, String ext, boolean both)
		{
			this.descr = descr;
			this.name = name;
			this.ext = ext;
			this.both = both;
		}
		
		/**
		 * @see FileFilter#accept(java.io.File)
		 */
		public boolean accept(File f)
		{
			if (f.isDirectory())
				return true;
			if (name == null && ext == null)
				return true;
			String fname = f.getName();
			if (fname.endsWith(ext))
			{
				if (both)
				{
					return fname.startsWith(name);
				}
				return true;
			}
			return false;
		}
		
		/**
		 * @see FileFilter#getDescription()
		 */
		public String getDescription()
		{
			return descr;
		}
	}
	
	/**
	 * Thread taking care for a interruptable emulation
	 *
	 * @author Patrick Schmidt
	 */
	private class Runner extends Thread
	{
		
		/**
		 * The mode in which this runner is running:
		 * true for normal mode
		 * false for statistic
		 */
		private final boolean mode;
		
		/**
		 * Flag signalizing whether this Thread is running
		 */
		private boolean running = true;
		
		/**
		 * Running the thread in normal mode
		 */
		private void runNormal()
		{
			ENDLESS:
			while (true)
			{
				for (int a = 0; a < instPerRun; a++)
				{
					if (!emulator.step(true))
					{
						updateButtons(MODE_ERROR);
						break ENDLESS;
					}
					if (!isRunning())
						break ENDLESS;
				}
				Thread.yield();
			}
		}
		
		/**
		 * Running the thread in statistics mode
		 */
		private void runStatistic()
		{
			ENDLESS:
			while (true)
			{
				for (int a = 0; a < instPerRun; a++)
				{
					if (!emulator.step(true))
					{
						updateButtons(MODE_ERROR);
						break ENDLESS;
					}
					if (!isRunning())
						break ENDLESS;
					//search current IP with corresponding current method
					methStatCur.numberOfExecIns++;
					numberOfExecIns++;
					currIP = emulator.getCurrentIP();
					if (currIP < methStatCur.firstIP || currIP > methStatCur.lastIP)
					{
						int startOfMthd = emulator.getStartOfMethod(currIP);
						methStatCur = methStatMain.getMethodStatistics(startOfMthd);
						// instruction not found in already parsed lists
						if (methStatCur == null)
						{
							// create new statistics
							methStatCur = new MethodStatistics(startOfMthd - emulator.codeStart, startOfMthd, emulator.getEndOfMethod(currIP));
							methStatMain.addMethodStatistic(methStatCur);
						}
						// determine whether this is a jump into the method or a call
						if (currIP == methStatCur.firstIP)
							methStatCur.numberOfCalls++;
					}
				}
				Thread.yield();
			}
		}
		
		public Runner(boolean mode)
		{
			this.mode = mode;
		}
		
		/**
		 * Method to change the status of this Thread
		 *
		 * @param r true for running, false to stop
		 */
		public synchronized void setRunning(boolean r)
		{
			running = r;
		}
		
		/**
		 * Method to obtain the status of this Thread
		 *
		 * @return true for running, false otherwise
		 */
		public synchronized boolean isRunning()
		{
			return running;
		}
		
		/**
		 * @see Thread#run()
		 */
		public void run()
		{
			if (mode)
				runNormal();
			else
				runStatistic();
			updateMethodView();
		}
	}
	
	/**
	 * Component enabling line highlighting with a rectangle
	 *
	 * @author Patrick Schmidt
	 * @version 060613 initial version
	 */
	private class SelectableArea extends JTextArea
	{
		
		public static final long serialVersionUID = 16047801002L;
		
		/**
		 * The currently selected line
		 */
		private int curLine;
		
		/**
		 * Flag to signalize whether the selection should be displayed or
		 * not
		 */
		private boolean showSelection;
		
		/**
		 * Standard constructor
		 */
		public SelectableArea()
		{
			setEditable(false);
		}
		
		/**
		 * @see JComponent#paint(java.awt.Graphics)
		 */
		public void paint(Graphics g)
		{
			super.paint(g);
			if (showSelection)
			{
				g.setColor(Color.RED);
				g.drawRect(0, curLine * getRowHeight(), getWidth() - 1, getRowHeight());
			}
		}
		
		/**
		 * Method to switch the selection to the next line
		 * (needed for repaint)
		 */
		public void step()
		{
			curLine++;
			repaint();
		}
		
		/**
		 * Method to set the selected line (needed for repaint)
		 *
		 * @param line the line to select, 0 for no selection
		 */
		public void setSelectedLine(int line)
		{
			curLine = line;
			repaint();
		}
		
		/**
		 * Method to obtain the line corresponding to the y-position in the
		 * component
		 *
		 * @param y the y-position in the component
		 * @return the corresponding line
		 */
		private int getLine(int y)
		{
			if (y < 0 || y > getHeight())
				return -1;
			return y / getRowHeight();
		}
	}
	
	/**
	 * Output for errors
	 *
	 * @author Patrick Schmidt
	 * @version 060614 initial version
	 */
	private class LogOut extends TextPrinter
	{
		
		/**
		 * Flag to determine whether the label in the toolbar should be cleared
		 */
		private boolean newStat = true;
		
		/**
		 * @see TextPrinter#close()
		 */
		public void close()
		{
			// nothing to do
		}
		
		/**
		 * @see TextPrinter#print(char)
		 */
		public void print(char c)
		{
			log.append(Character.toString(c));
			if (newStat)
			{
				newStat = false;
				logLabel.setText(Character.toString(c));
			}
			else
				logLabel.setText(logLabel.getText().concat(Character.toString(c)));
		}
		
		/**
		 * @see TextPrinter#print(int)
		 */
		public void print(int i)
		{
			log.append(Integer.toString(i));
			if (newStat)
			{
				newStat = false;
				logLabel.setText(Integer.toString(i));
			}
			else
				logLabel.setText(logLabel.getText().concat(Integer.toString(i)));
		}
		
		/**
		 * @see TextPrinter#print(long)
		 */
		public void print(long l)
		{
			log.append(Long.toString(l));
			if (newStat)
			{
				newStat = false;
				logLabel.setText(Long.toString(l));
			}
			else
				logLabel.setText(logLabel.getText().concat(Long.toString(l)));
		}
		
		/**
		 * @see TextPrinter#print(String)
		 */
		public void print(String s)
		{
			log.append(s);
			if (newStat)
			{
				newStat = false;
				logLabel.setText(s);
			}
			else
				logLabel.setText(logLabel.getText().concat(s));
		}
		
		/**
		 * @see TextPrinter#println()
		 */
		public void println()
		{
			log.append("\n");
			log.setCaretPosition(log.getDocument().getLength());
			newStat = true;
		}
		
	}
	
	/**
	 * JDialog for setting options
	 *
	 * @author Patrick Schmidt
	 * @version 060622 end of main added
	 * version 060619 initial version
	 */
	private class OptionDialog extends Dialogues
	{
		
		public final static long serialVersionUID = 0x1604780101l;
		
		/**
		 * Label and action command for the ok button
		 */
		private static final String OK_B = "ok";
		
		/**
		 * Label and action command for the abort button
		 */
		private static final String ABORT_B = "abort";
		
		/**
		 * Standard constructor setting the parent container
		 *
		 * @param parent the parent container
		 */
		public OptionDialog(Frame parent)
		{
			super(parent, "Options");
		}
		
		/**
		 * Textfield for instructions per thread
		 */
		private JTextField instrPerThread;
		
		/**
		 * @see Dialogues#init()
		 */
		protected void init()
		{
			// init panels for widgets
			JPanel widgets = new JPanel(new GridLayout(1, 2, 5, 5));
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
			
			// set layout of dialog
			setLayout(new BorderLayout());
			
			// init widgets
			instrPerThread = new JTextField();
			instrPerThread.setPreferredSize(new Dimension(50, 25));
			
			// add widgets to the panels
			widgets.add(new JLabel("Instructions per Thread-run:"));
			widgets.add(instrPerThread);
			buttons.add(createButton(OK_B, this, BUTTONS));
			buttons.add(createButton(ABORT_B, this, BUTTONS));
			
			// add panels to the dialog
			add(widgets, BorderLayout.CENTER);
			add(buttons, BorderLayout.SOUTH);
			
			setSize(350, 80);
		}
		
		/**
		 * @see Dialogues#processButton(String)
		 */
		protected void processButton(String actionCommand)
		{
			if (actionCommand.equals(OK_B))
				instPerRun = Integer.parseInt(instrPerThread.getText());
			setVisible(false);
		}
		
		/**
		 * @see Dialogues#setVisible()
		 */
		protected void setVisible()
		{
			instrPerThread.setText(Integer.toString(instPerRun));
			instrPerThread.requestFocus();
		}
	}
	
	/**
	 * JDialog containing the registered breakpoints
	 *
	 * @author Patrick Schmidt
	 * @version 060620 initial version
	 */
	private class BreakPoints extends Dialogues
	{
		
		public static final long serialVersionUID = 16047801002L;
		
		/**
		 * Tablemodel for the breakpoints dialog
		 *
		 * @author Patrick Schmidt
		 * @version 060621 initial version
		 */
		private class MemBreakPointTableData implements TableModel
		{
			
			/**
			 * @see TableModel#addTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void addTableModelListener(TableModelListener l)
			{
				// nothing to do
			}
			
			/**
			 * @see TableModel#getColumnClass(int)
			 */
			public Class<Condition> getColumnClass(int columnIndex)
			{
				return Condition.class;
			}
			
			/**
			 * @see TableModel#getColumnCount()
			 */
			public int getColumnCount()
			{
				return 1;
			}
			
			/**
			 * @see TableModel#getColumnName(int)
			 */
			public String getColumnName(int columnIndex)
			{
				return "Registered breakpoints";
			}
			
			/**
			 * @see TableModel#getRowCount()
			 */
			public int getRowCount()
			{
				if (emulator == null)
					return 0;
				int cnt = 0;
				Condition c = emulator.firstBreakPointC;
				while (c != null)
				{
					c = c.next;
					cnt++;
				}
				c = emulator.firstMemC;
				while (c != null)
				{
					c = c.next;
					cnt++;
				}
				return cnt;
			}
			
			/**
			 * @see TableModel#getValueAt(int, int)
			 */
			public Object getValueAt(int rowIndex, int columnIndex)
			{
				if (columnIndex != 0 || emulator == null)
					return null;
				int cnt = 0;
				Condition temp = emulator.firstBreakPointC;
				while (temp != null)
				{
					if (cnt == rowIndex)
						return temp;
					temp = temp.next;
					cnt++;
				}
				temp = emulator.firstMemC;
				while (temp != null)
				{
					if (cnt == rowIndex)
						return temp;
					temp = temp.next;
					cnt++;
				}
				return null;
			}
			
			/**
			 * @see TableModel#isCellEditable(int, int)
			 */
			public boolean isCellEditable(int rowIndex, int columnIndex)
			{
				return false;
			}
			
			/**
			 * @see TableModel#removeTableModelListener(
			 *javax.swing.event.TableModelListener)
			 */
			public void removeTableModelListener(TableModelListener l)
			{
				// nothing to do
			}
			
			/**
			 * @see TableModel#setValueAt(java.lang.Object, int, int)
			 */
			public void setValueAt(Object aValue, int rowIndex, int columnIndex)
			{
				// nothing to do
			}
		}
		
		/**
		 * Dialog for adding memory breakpoints
		 *
		 * @author Patrick Schmidt
		 * @version 060621 initial version
		 */
		private class MemBreakPoint extends Dialogues
		{
			
			public final static long serialVersionUID = 16047801001l;
			
			/**
			 * Label and action command for the save button
			 */
			public static final String B_SAVE = "save";
			
			/**
			 * Label and action command for the cancel button
			 */
			public static final String B_CANCEL = "cancel";
			
			/**
			 * Textfield for the observed memory
			 */
			private JTextField address;
			
			/**
			 * Radiobutton for memory type IO
			 */
			private JRadioButton radioIO;
			
			/**
			 * Radiobutton for memory access read
			 */
			private JRadioButton radioRead;
			
			/**
			 * Radiobutton for memory access write
			 */
			private JRadioButton radioWrite;
			
			/**
			 * Radiobutton for memory type RAM
			 */
			private JRadioButton radioRAM;
			
			/**
			 * Radiobutton for memory access write and read
			 */
			private JRadioButton radioBoth;
			
			/**
			 * Standard constructor
			 *
			 * @param parent the parent container
			 */
			public MemBreakPoint(JDialog parent)
			{
				super(parent, "Add memory condition");
			}
			
			/**
			 * @see Dialogues#init()
			 */
			protected void init()
			{
				// initializing widgets
				JPanel north = new JPanel(new GridLayout(1, 2, 5, 5));
				address = new JTextField();
				north.add(new JLabel("Address to observe:"));
				north.add(address);
				
				// Radiogroup for memory type
				JPanel memGroup = new JPanel(new FlowLayout(FlowLayout.LEFT));
				memGroup.setBorder(new TitledBorder("Memory space"));
				// radio buttons for memGroup
				radioRAM = new JRadioButton("RAM", true);
				radioIO = new JRadioButton("IO");
				memGroup.add(radioRAM);
				memGroup.add(radioIO);
				// ButtonGroup for exclusive selection
				ButtonGroup memType = new ButtonGroup();
				memType.add(radioRAM);
				memType.add(radioIO);
				
				// Radiogroup for access type
				JPanel accessGroup = new JPanel(new FlowLayout(FlowLayout.LEFT));
				accessGroup.setBorder(new TitledBorder("Type of access"));
				// radio buttons for accessGroup
				radioRead = new JRadioButton("read", true);
				radioWrite = new JRadioButton("write");
				radioBoth = new JRadioButton("both");
				accessGroup.add(radioRead);
				accessGroup.add(radioWrite);
				accessGroup.add(radioBoth);
				// ButtonGroup for exclusive selection
				ButtonGroup access = new ButtonGroup();
				access.add(radioRead);
				access.add(radioWrite);
				access.add(radioBoth);
				
				// center panel in this dialog
				JPanel center = new JPanel(new GridLayout(1, 2, 5, 5));
				center.add(memGroup);
				center.add(accessGroup);
				
				// south panel containing the buttons
				JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
				south.add(createButton(B_SAVE, this, BUTTONS));
				south.add(createButton(B_CANCEL, this, BUTTONS));
				
				setLayout(new BorderLayout());
				add(north, BorderLayout.NORTH);
				add(center, BorderLayout.CENTER);
				add(south, BorderLayout.SOUTH);
				
				setSize(380, 130);
			}
			
			/**
			 * @see Dialogues#processButton(String)
			 */
			protected void processButton(String actionCommand)
			{
				if (actionCommand.equals(B_SAVE))
				{
					Condition c;
					int addr = Integer.parseInt(address.getText());
					boolean memType = radioIO.isSelected();
					if (radioBoth.isSelected())
					{
						c = new MemoryBreak(addr, memType, true);
						c.next = emulator.firstMemC;
						emulator.firstMemC = c;
						c = new MemoryBreak(addr, memType, false);
					}
					else
					{
						c = new MemoryBreak(addr, memType, radioRead.isSelected());
					}
					c.next = emulator.firstMemC;
					emulator.firstMemC = c;
				}
				setVisible(false);
			}
			
			/**
			 * @see Dialogues#setVisible()
			 */
			protected void setVisible()
			{
				address.setText("");
				address.requestFocus();
				radioRAM.setSelected(true);
				radioRead.setSelected(true);
			}
		}
		
		/**
		 * Label and action command for the delete button
		 */
		public static final String B_DELETE = "delete";
		
		/**
		 * Label and action command for the close button
		 */
		public static final String B_CLOSE = "close";
		
		/**
		 * Label and action command for the mem condition button
		 */
		public static final String B_MEM = "mem cond.";
		
		/**
		 * JTable containing the data
		 */
		private JTable breakpoints;
		
		/**
		 * Button to delete breakpoints
		 */
		private JButton deleteB;
		
		/**
		 * The line currently selected in the JTable
		 */
		private int selectedLine;
		
		/**
		 * Dialog for adding memory breakpoints
		 */
		private MemBreakPoint addMemBreakPoint;
		
		/**
		 * Standard constructor
		 *
		 * @param parent the parent component
		 */
		public BreakPoints(Frame parent)
		{
			super(parent, "Breakpoints");
		}
		
		protected void init()
		{
			setLayout(new BorderLayout());
			
			// init JTable with TableModel and add it to the Frame
			breakpoints = new JTable(new MemBreakPointTableData());
			add(new JScrollPane(breakpoints), BorderLayout.CENTER);
			
			// create buttons and add them to the buttons panel, finally add panel to
			// frame
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
			deleteB = createButton(B_DELETE, this, BUTTONS);
			buttons.add(createButton(B_MEM, this, BUTTONS));
			buttons.add(deleteB);
			buttons.add(createButton(B_CLOSE, this, BUTTONS));
			add(buttons, BorderLayout.SOUTH);
			
			// create dialog for adding memory breakpoints
			addMemBreakPoint = new MemBreakPoint(this);
			addMemBreakPoint.init();
			setSize(300, 200);
		}
		
		protected void processButton(String actionCommand)
		{
			if (actionCommand.equals(B_CLOSE))
				setVisible(false);
			else if (actionCommand.equals(B_DELETE))
			{
				// get the selected item
				selectedLine = breakpoints.getSelectedRow();
				if (selectedLine == -1)
					return;
				// delete the condition from the list
				Object o = breakpoints.getModel().getValueAt(selectedLine, 0);
				if (emulator.firstBreakPointC == o)
					emulator.firstBreakPointC = emulator.firstBreakPointC.next;
				else if (emulator.firstMemC == o)
					emulator.firstMemC = emulator.firstMemC.next;
				else
				{
					boolean goOn = true;
					// check the break conditions
					Condition temp = emulator.firstBreakPointC;
					while (goOn)
					{
						if (temp == null)
							break;
						if (temp.next == o)
						{
							temp.next = temp.next.next;
							goOn = false;
						}
						temp = temp.next;
					}
					if (goOn)
					{
						temp = emulator.firstMemC;
						while (goOn)
						{
							if (temp == null)
								break;
							if (temp.next == o)
							{
								temp.next = temp.next.next;
								goOn = false;
							}
							temp = temp.next;
						}
					}
				}
				breakpoints.tableChanged(null);
				// disable button
				if (breakpoints.getRowCount() == 0)
					deleteB.setEnabled(false);
			}
			else if (actionCommand.equals(B_MEM))
			{
				addMemBreakPoint.setVisible(true);
			}
		}
		
		protected void setVisible()
		{
			// update the selection and the data displayed
			breakpoints.tableChanged(null);
		}
	}
	
	public static class GUIConsole extends JComponent implements TextScreen
	{
		public final static long serialVersionUID = 16047801002l;
		
		private final static byte[] F8x16 = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x81, (byte) 0xa5, (byte) 0x81, (byte) 0x81, (byte) 0xbd, (byte) 0x99, (byte) 0x81, (byte) 0x81, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0xff, (byte) 0xdb, (byte) 0xff, (byte) 0xff, (byte) 0xc3, (byte) 0xe7, (byte) 0xff, (byte) 0xff, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x6c, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0x7c, (byte) 0x38, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x7c, (byte) 0xfe, (byte) 0x7c, (byte) 0x38, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x3c, (byte) 0xe7, (byte) 0xe7, (byte) 0xe7, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x7e, (byte) 0xff, (byte) 0xff, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x3c, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xe7, (byte) 0xc3, (byte) 0xc3, (byte) 0xe7, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x66, (byte) 0x42, (byte) 0x42, (byte) 0x66, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc3, (byte) 0x99, (byte) 0xbd, (byte) 0xbd, (byte) 0x99, (byte) 0xc3, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x1e, (byte) 0x0e, (byte) 0x1a, (byte) 0x32, (byte) 0x78, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x78, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3f, (byte) 0x33, (byte) 0x3f, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x70, (byte) 0xf0, (byte) 0xe0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7f, (byte) 0x63, (byte) 0x7f, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x67, (byte) 0xe7, (byte) 0xe6, (byte) 0xc0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0xdb, (byte) 0x3c, (byte) 0xe7, (byte) 0x3c, (byte) 0xdb, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0xc0, (byte) 0xe0, (byte) 0xf0, (byte) 0xf8, (byte) 0xfe, (byte) 0xf8, (byte) 0xf0, (byte) 0xe0, (byte) 0xc0, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x06, (byte) 0x0e, (byte) 0x1e, (byte) 0x3e, (byte) 0xfe, (byte) 0x3e, (byte) 0x1e, (byte) 0x0e, (byte) 0x06, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0x3c, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x00, (byte) 0x66, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7f, (byte) 0xdb, (byte) 0xdb, (byte) 0xdb, (byte) 0x7b, (byte) 0x1b, (byte) 0x1b, (byte) 0x1b, (byte) 0x1b, (byte) 0x1b, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0x60, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0xc6, (byte) 0x6c, (byte) 0x38, (byte) 0x0c, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0x3c, (byte) 0x18, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0x3c, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x0c, (byte) 0xfe, (byte) 0x0c, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x60, (byte) 0xfe, (byte) 0x60, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x24, (byte) 0x66, (byte) 0xff, (byte) 0x66, (byte) 0x24, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x38, (byte) 0x7c, (byte) 0x7c, (byte) 0xfe, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xfe, (byte) 0x7c, (byte) 0x7c, (byte) 0x38, (byte) 0x38, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x24, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x6c, (byte) 0x6c, (byte) 0xfe, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0xfe, (byte) 0x6c, (byte) 0x6c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x7c, (byte) 0xc6, (byte) 0xc2, (byte) 0xc0, (byte) 0x7c, (byte) 0x06, (byte) 0x06, (byte) 0x86, (byte) 0xc6, (byte) 0x7c, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc2, (byte) 0xc6, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xc6, (byte) 0x86, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0x6c, (byte) 0x38, (byte) 0x76, (byte) 0xdc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x60, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x18, (byte) 0x0c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x18, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x66, (byte) 0x3c, (byte) 0xff, (byte) 0x3c, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x06, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xc0, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x66, (byte) 0xc3, (byte) 0xc3, (byte) 0xdb, (byte) 0xdb, (byte) 0xc3, (byte) 0xc3, (byte) 0x66, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x38, (byte) 0x78, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0x06, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xc0, (byte) 0xc6, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0x06, (byte) 0x06, (byte) 0x3c, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x1c, (byte) 0x3c, (byte) 0x6c, (byte) 0xcc, (byte) 0xfe, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x1e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xfc, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x60, (byte) 0xc0, (byte) 0xc0, (byte) 0xfc, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xc6, (byte) 0x06, (byte) 0x06, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7e, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0x0c, (byte) 0x78, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x0c, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x0c, (byte) 0x06, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0x0c, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xde, (byte) 0xde, (byte) 0xde, (byte) 0xdc, (byte) 0xc0, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0xc6, (byte) 0xfe, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfc, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x7c, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0xfc, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x66, (byte) 0xc2, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc2, (byte) 0x66, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf8, (byte) 0x6c, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x6c, (byte) 0xf8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x66, (byte) 0x62, (byte) 0x68, (byte) 0x78, (byte) 0x68, (byte) 0x60, (byte) 0x62, (byte) 0x66, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x66, (byte) 0x62, (byte) 0x68, (byte) 0x78, (byte) 0x68, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x66, (byte) 0xc2, (byte) 0xc0, (byte) 0xc0, (byte) 0xde, (byte) 0xc6, (byte) 0xc6, (byte) 0x66, (byte) 0x3a, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xfe, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1e, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x78, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xe6, (byte) 0x66, (byte) 0x66, (byte) 0x6c, (byte) 0x78, (byte) 0x78, (byte) 0x6c, (byte) 0x66, (byte) 0x66, (byte) 0xe6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf0, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x62, (byte) 0x66, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0xe7, (byte) 0xff, (byte) 0xff, (byte) 0xdb, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0xe6, (byte) 0xf6, (byte) 0xfe, (byte) 0xde, (byte) 0xce, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfc, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x7c, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xd6, (byte) 0xde, (byte) 0x7c, (byte) 0x0c, (byte) 0x0e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfc, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x7c, (byte) 0x6c, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0xe6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0x60, (byte) 0x38, (byte) 0x0c, (byte) 0x06, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xdb, (byte) 0x99, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xdb, (byte) 0xdb, (byte) 0xff, (byte) 0x66, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0xc3, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x66, (byte) 0xc3, (byte) 0xc3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xc3, (byte) 0x86, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xc1, (byte) 0xc3, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0xc0, (byte) 0xe0, (byte) 0x70, (byte) 0x38, (byte) 0x1c, (byte) 0x0e, (byte) 0x06, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x0c, (byte) 0x7c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xe0, (byte) 0x60, (byte) 0x60, (byte) 0x78, (byte) 0x6c, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1c, (byte) 0x0c, (byte) 0x0c, (byte) 0x3c, (byte) 0x6c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xfe, (byte) 0xc0, (byte) 0xc0, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0x64, (byte) 0x60, (byte) 0xf0, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x7c, (byte) 0x0c, (byte) 0xcc, (byte) 0x78, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xe0, (byte) 0x60, (byte) 0x60, (byte) 0x6c, (byte) 0x76, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0xe6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x38, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x06, (byte) 0x00, (byte) 0x0e, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0x66, (byte) 0x66, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xe0, (byte) 0x60, (byte) 0x60, (byte) 0x66, (byte) 0x6c, (byte) 0x78, (byte) 0x78, (byte) 0x6c, (byte) 0x66, (byte) 0xe6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xe6, (byte) 0xff, (byte) 0xdb, (byte) 0xdb, (byte) 0xdb, (byte) 0xdb, (byte) 0xdb, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xdc, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xdc, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x7c, (byte) 0x60, (byte) 0x60, (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x7c, (byte) 0x0c, (byte) 0x0c, (byte) 0x1e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xdc, (byte) 0x76, (byte) 0x66, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0x60, (byte) 0x38, (byte) 0x0c, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x30, (byte) 0x30, (byte) 0xfc, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x36, (byte) 0x1c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0xc3, (byte) 0xc3, (byte) 0xdb, (byte) 0xdb, (byte) 0xff, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0x3c, (byte) 0x66, (byte) 0xc3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7e, (byte) 0x06, (byte) 0x0c, (byte) 0xf8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xcc, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xc6, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x70, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x0e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x70, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x0e, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x70, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xdc, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x66, (byte) 0xc2, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc2, (byte) 0x66, (byte) 0x3c, (byte) 0x0c, (byte) 0x06, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xcc, (byte) 0x00, (byte) 0x00, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xfe, (byte) 0xc0, (byte) 0xc0, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x6c, (byte) 0x00, (byte) 0x78, (byte) 0x0c, (byte) 0x7c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xcc, (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x0c, (byte) 0x7c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x00, (byte) 0x78, (byte) 0x0c, (byte) 0x7c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0x38, (byte) 0x00, (byte) 0x78, (byte) 0x0c, (byte) 0x7c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x66, (byte) 0x60, (byte) 0x60, (byte) 0x66, (byte) 0x3c, (byte) 0x0c, (byte) 0x06, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x6c, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xfe, (byte) 0xc0, (byte) 0xc0, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xfe, (byte) 0xc0, (byte) 0xc0, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xfe, (byte) 0xc0, (byte) 0xc0, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x3c, (byte) 0x66, (byte) 0x00, (byte) 0x38, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x00, (byte) 0x38, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0xc6, (byte) 0xfe, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0x38, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0xc6, (byte) 0xfe, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x00, (byte) 0xfe, (byte) 0x66, (byte) 0x60, (byte) 0x7c, (byte) 0x60, (byte) 0x60, (byte) 0x66, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x6e, (byte) 0x3b, (byte) 0x1b, (byte) 0x7e, (byte) 0xd8, (byte) 0xdc, (byte) 0x77, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3e, (byte) 0x6c, (byte) 0xcc, (byte) 0xcc, (byte) 0xfe, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xce, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x38, (byte) 0x6c, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x78, (byte) 0xcc, (byte) 0x00, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x00, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7e, (byte) 0x06, (byte) 0x0c, (byte) 0x78, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc6, (byte) 0x00, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0xc3, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc3, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0x64, (byte) 0x60, (byte) 0xf0, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0xe6, (byte) 0xfc, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc3, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0xff, (byte) 0x18, (byte) 0xff, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfc, (byte) 0x66, (byte) 0x66, (byte) 0x7c, (byte) 0x62, (byte) 0x66, (byte) 0x6f, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0xf3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x1b, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xd8, (byte) 0x70, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x00, (byte) 0x78, (byte) 0x0c, (byte) 0x7c, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x38, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x00, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xdc, (byte) 0x00, (byte) 0xdc, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xdc, (byte) 0x00, (byte) 0xc6, (byte) 0xe6, (byte) 0xf6, (byte) 0xfe, (byte) 0xde, (byte) 0xce, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x6c, (byte) 0x6c, (byte) 0x3e, (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0x6c, (byte) 0x38, (byte) 0x00, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x60, (byte) 0xc0, (byte) 0xc6, (byte) 0xc6, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc0, (byte) 0xc0, (byte) 0xc2, (byte) 0xc6, (byte) 0xcc, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xce, (byte) 0x9b, (byte) 0x06, (byte) 0x0c, (byte) 0x1f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc0, (byte) 0xc0, (byte) 0xc2, (byte) 0xc6, (byte) 0xcc, (byte) 0x18, (byte) 0x30, (byte) 0x66, (byte) 0xce, (byte) 0x96, (byte) 0x3e, (byte) 0x06, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x36, (byte) 0x6c, (byte) 0xd8, (byte) 0x6c, (byte) 0x36, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd8, (byte) 0x6c, (byte) 0x36, (byte) 0x6c, (byte) 0xd8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0x44, (byte) 0x11, (byte) 0x44, (byte) 0x11, (byte) 0x44, (byte) 0x11, (byte) 0x44, (byte) 0x11, (byte) 0x44, (byte) 0x11, (byte) 0x44, (byte) 0x11, (byte) 0x44, (byte) 0x11, (byte) 0x44, (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa, (byte) 0xdd, (byte) 0x77, (byte) 0xdd, (byte) 0x77, (byte) 0xdd, (byte) 0x77, (byte) 0xdd, (byte) 0x77, (byte) 0xdd, (byte) 0x77, (byte) 0xdd, (byte) 0x77, (byte) 0xdd, (byte) 0x77, (byte) 0xdd, (byte) 0x77, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xf8, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xf8, (byte) 0x18, (byte) 0xf8, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xf6, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf8, (byte) 0x18, (byte) 0xf8, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xf6, (byte) 0x06, (byte) 0xf6, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x06, (byte) 0xf6, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xf6, (byte) 0x06, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xf8, (byte) 0x18, (byte) 0xf8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf8, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x1f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x1f, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xff, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x1f, (byte) 0x18, (byte) 0x1f, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x37, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x37, (byte) 0x30, (byte) 0x3f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3f, (byte) 0x30, (byte) 0x37, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xf7, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0xf7, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x37, (byte) 0x30, (byte) 0x37, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xf7, (byte) 0x00, (byte) 0xf7, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xff, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0xff, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x3f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x1f, (byte) 0x18, (byte) 0x1f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1f, (byte) 0x18, (byte) 0x1f, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3f, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0xff, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x36, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xff, (byte) 0x18, (byte) 0xff, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xf8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1f, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0x0f, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xdc, (byte) 0xd8, (byte) 0xd8, (byte) 0xd8, (byte) 0xdc, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xd8, (byte) 0xcc, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xcc, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xc6, (byte) 0xc6, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0xc0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0xc6, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xc6, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0xd8, (byte) 0xd8, (byte) 0xd8, (byte) 0xd8, (byte) 0xd8, (byte) 0x70, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x7c, (byte) 0x60, (byte) 0x60, (byte) 0xc0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xdc, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x18, (byte) 0x3c, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x3c, (byte) 0x18, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0xc6, (byte) 0xfe, (byte) 0xc6, (byte) 0xc6, (byte) 0x6c, (byte) 0x38, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0xee, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1e, (byte) 0x30, (byte) 0x18, (byte) 0x0c, (byte) 0x3e, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0xdb, (byte) 0xdb, (byte) 0xdb, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x06, (byte) 0x7e, (byte) 0xdb, (byte) 0xdb, (byte) 0xf3, (byte) 0x7e, (byte) 0x60, (byte) 0xc0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1c, (byte) 0x30, (byte) 0x60, (byte) 0x60, (byte) 0x7c, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x30, (byte) 0x1c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0xc6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0xfe, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x7e, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x18, (byte) 0x0c, (byte) 0x06, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x0c, (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x1b, (byte) 0x1b, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xd8, (byte) 0xd8, (byte) 0xd8, (byte) 0x70, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xdc, (byte) 0x00, (byte) 0x76, (byte) 0xdc, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x6c, (byte) 0x6c, (byte) 0x38, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0x0c, (byte) 0xec, (byte) 0x6c, (byte) 0x6c, (byte) 0x3c, (byte) 0x1c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xd8, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x6c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x70, (byte) 0xd8, (byte) 0x30, (byte) 0x60, (byte) 0xc8, (byte) 0xf8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7c, (byte) 0x7c, (byte) 0x7c, (byte) 0x7c, (byte) 0x7c, (byte) 0x7c, (byte) 0x7c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
		
		private final static int charWidth = 8, charHeight = 16;
		
		private final int col;
		private final int row;
		private final char[][] txt;
		private final int[][] colFG;
		private final int[][] colBG;
		private final Color[] colTable;
		
		private Image backImage;
		
		public GUIConsole(int iw, int ih)
		{
			super();
			
			int x, y;
			
			txt = new char[ih][iw];
			colFG = new int[ih][iw];
			colBG = new int[ih][iw];
			for (y = 0; y < ih; y++)
				for (x = 0; x < iw; x++)
				{
					txt[y][x] = ' ';
					colFG[y][x] = 7; //colBG is initialized to 0
				}
			colTable = new Color[16];
			colTable[0] = new Color(0x000000); //black
			colTable[1] = new Color(0x00007F); //blue
			colTable[2] = new Color(0x007F00); //green
			colTable[3] = new Color(0x007F7F); //cyan
			colTable[4] = new Color(0x7F0000); //red
			colTable[5] = new Color(0x7F007F); //magenta
			colTable[6] = new Color(0x7F7F00); //brown
			colTable[7] = new Color(0x7F7F7F); //white
			colTable[8] = new Color(0x4F4F4F); //gray
			colTable[9] = new Color(0x0000FF); //bright blue
			colTable[10] = new Color(0x00FF00); //bright green
			colTable[11] = new Color(0x00FFFF); //bright cyan
			colTable[12] = new Color(0xFF0000); //bright red
			colTable[13] = new Color(0xFF00FF); //bright magenta
			colTable[14] = new Color(0xFFFF00); //bright yellow
			colTable[15] = new Color(0xFFFFFF); //bright white
			col = iw;
			row = ih;
			setSize(charWidth * col, charHeight * row);
		}
		
		public void setChar(int x, int y, char c)
		{
			txt[y][x] = c;
		}
		
		public void setColor(int x, int y, byte col)
		{
			colFG[y][x] = (int) col & 0x0F;
			colBG[y][x] = ((int) col & 0xF0) >>> 4;
		}
		
		public int getWidth()
		{
			return charWidth * col;
		}
		
		public int getHeight()
		{
			return charHeight * row;
		}
		
		public Dimension getPreferredSize()
		{
			return new Dimension(getWidth(), getHeight());
		}
		
		public void redraw()
		{
			if (backImage != null)
			{
				redraw(backImage.getGraphics());
				repaint();
			}
		}
		
		public void redraw(int x, int y)
		{
			if (backImage != null)
			{
				drawChar(backImage.getGraphics(), x, y);
				repaint();
			}
		}
		
		public void paint(Graphics g)
		{
			Dimension size;
			
			if (backImage == null)
			{
				size = super.getSize();
				backImage = createImage(size.width, size.height);
				redraw();
			}
			g.drawImage(backImage, 0, 0, this);
		}
		
		private void redraw(Graphics g)
		{
			for (int y = 0; y < row; y++)
				for (int x = 0; x < col; x++)
					drawChar(g, x, y);
		}
		
		private void drawChar(Graphics g, int x, int y)
		{
			int sx = x * charWidth, sy = y * charHeight, base;
			Color fg = colTable[colFG[y][x]], bg = colTable[colBG[y][x]];
			
			base = ((int) txt[y][x] & 0xFF) * charHeight;
			for (int j = 0; j < charHeight; j++)
			{
				for (int i = 0; i < charWidth; i++)
				{
					g.setColor((F8x16[base] & (1 << (7 - i))) != 0 ? fg : bg);
					g.drawLine(sx + i, sy + j, sx + i, sy + j);
				}
				base++;
			}
		}
	}
	
	public final static long serialVersionUID = 16047801001l;
	
	/**
	 * Size of the console
	 */
	public final static int width = 80, height = 25;
	
	/**
	 * Label and action command for the load button
	 */
	private static final String B_LOAD = "load";
	
	/**
	 * Label and action command for the start button
	 */
	private static final String B_START = "start";
	
	/**
	 * Label and action command for the stop button
	 */
	private static final String B_STOP = "stop";
	
	/**
	 * Label and action command for the single into button
	 */
	private static final String B_SINGLE_INTO = "single into";
	
	/**
	 * Label and action command for the single over button
	 */
	private static final String B_SINGLE_OVER = "single over";
	
	/**
	 * Label and action command for the quit button
	 */
	private static final String B_QUIT = "quit";
	
	/**
	 * Label and action command for the options button
	 */
	private static final String B_OPTIONS = "options";
	
	/**
	 * Label and action command for statistics button
	 */
	private static final String B_STATISTIC = "statistics";
	
	/**
	 * Label and action command for the breakpoints button
	 */
	private static final String B_BREAKPOINTS = "breakpoints";
	
	private static final String B_EXPORT = "export";
	
	/**
	 * Console widget
	 */
	private final GUIConsole out;
	
	/**
	 * Textarea for disassembling information
	 */
	private final SelectableArea disassWindow;
	
	/**
	 * Textarea for log outputs
	 */
	private final JTextArea log;
	
	/**
	 * Instance of the emulator
	 */
	private Emulator emulator;
	
	/**
	 * Filters for the JFileChooser
	 */
	private final Filter[] fileFilters;
	
	/**
	 * File chooser for images to load in the emulator
	 */
	private final JFileChooser chooser;
	
	/**
	 * Thread for emulating
	 */
	private Runner runner;
	
	/**
	 * The current disassembled method
	 */
	private MethodDisassembly currMethod;
	
	/**
	 * The disassembled main method of this program
	 */
	private MethodDisassembly main;
	
	/**
	 * The next instruction
	 */
	private Mnemonic nextIns;
	
	/**
	 * The current IP
	 */
	private int currIP;
	
	/**
	 * Label to print out the last status message
	 */
	private final JLabel logLabel;
	
	/**
	 * Button to start the emulation
	 */
	private final JButton startB;
	
	/**
	 * Button to stop the emulation
	 */
	private final JButton stopB;
	
	/**
	 * Button to step over
	 */
	private final JButton singleOverB;
	
	/**
	 * Button to step into
	 */
	private final JButton singleIntoB;
	
	/**
	 * Button for the options
	 */
	private final JButton optionsB;
	
	/**
	 * Button for the statistics
	 */
	private final JButton statisticB;
	
	/**
	 * Button for the breakpoints
	 */
	private final JButton breakpointB;
	
	/**
	 * How many instructions per loop in Thread run
	 */
	private int instPerRun = 1000;
	
	/**
	 * Dialog containing the options
	 */
	private final OptionDialog dialog;
	
	/**
	 * Dialog with the currently registered breakpoints
	 */
	private final BreakPoints breakPoints;
	
	/**
	 * Dialog for the statistics
	 */
	private final Statistics stats;
	
	/**
	 * Flag to signalize whether the runner should proceed
	 */
	private boolean proceed;
	
	/**
	 * Flag to determine in which mode the emulator runs
	 * True for running in normal mode
	 * False for statistics mode
	 */
	private boolean runNormal;
	
	/**
	 * Method statistics for main method
	 */
	private MethodStatistics methStatMain;
	
	/**
	 * Currently updated method statistic object
	 */
	private MethodStatistics methStatCur;
	
	/**
	 * Text printer for log out
	 */
	private TextPrinter tp;
	
	/**
	 * The number of currently executed instructions
	 */
	private int numberOfExecIns;
	
	/**
	 * Standard constructor
	 */
	public Emulate()
	{
		super("ssaEmulator");
		
		// declaration of toolsbars and panels
		JToolBar toolbar;
		JPanel consolePanel, centerPanel;
		
		setResizable(true);
		addKeyListener(this);
		
		//detect window close event
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		
		// layout for JFrame and JPanels with default gaps for height and width
		BorderLayout layout = new BorderLayout();
		layout.setHgap(3);
		layout.setVgap(3);
		setLayout(layout);
		
		// create and add buttons to JFrame
		toolbar = new JToolBar();
		startB = createButton(B_START, this, null);
		startB.setEnabled(false);
		singleOverB = createButton(B_SINGLE_OVER, this, null);
		singleOverB.setEnabled(false);
		singleIntoB = createButton(B_SINGLE_INTO, this, null);
		singleIntoB.setEnabled(false);
		stopB = createButton(B_STOP, this, null);
		stopB.setEnabled(false);
		optionsB = createButton(B_OPTIONS, this, null);
		statisticB = createButton(B_STATISTIC, this, null);
		statisticB.setEnabled(false);
		breakpointB = createButton(B_BREAKPOINTS, this, null);
		breakpointB.setEnabled(false);
		toolbar.add(createButton(B_QUIT, this, null));
		toolbar.add(createButton(B_LOAD, this, null));
		toolbar.add(startB);
		toolbar.add(singleOverB);
		toolbar.add(singleIntoB);
		toolbar.add(stopB);
		toolbar.add(statisticB);
		toolbar.add(optionsB);
		toolbar.add(breakpointB);
		toolbar.add(createButton(B_EXPORT, this, null));
		toolbar.setPreferredSize(new Dimension(100, 25));
		logLabel = new JLabel();
		toolbar.add(new JSeparator());
		toolbar.add(logLabel);
		add(toolbar, BorderLayout.NORTH);
		
		// init panels for arrangement
		// JPanel for console and log JTextArea with BorderLayout
		consolePanel = new JPanel();
		layout = new BorderLayout();
		layout.setHgap(3);
		layout.setVgap(3);
		consolePanel.setLayout(layout);
		// init log JTextArea with titled border and scrollbar
		log = new JTextArea();
		JScrollPane scrollLog = new JScrollPane(log);
		scrollLog.setBorder(new TitledBorder("Log"));
		// add widgets to consolePanel
		consolePanel.add(out = new GUIConsole(80, 25), BorderLayout.NORTH);
		consolePanel.add(scrollLog, BorderLayout.CENTER);
		
		// init panel for disassembling information and additional information
		centerPanel = new JPanel();
		layout = new BorderLayout();
		layout.setHgap(3);
		layout.setVgap(3);
		centerPanel.setLayout(layout);
		centerPanel.setBorder(new TitledBorder("Disassembler"));
		// init disassembling JTextArea with titled border and scrollbar
		disassWindow = new SelectableArea();
		disassWindow.setFont(new Font("Courier New", Font.PLAIN, 12));
		disassWindow.addMouseListener(this);
		JScrollPane scrollText = new JScrollPane(disassWindow);
		// add widgets to centerPanel
		centerPanel.add(scrollText, BorderLayout.CENTER);
		
		// add panels to JFrame and set size
		add(consolePanel, BorderLayout.WEST);
		add(centerPanel, BorderLayout.CENTER);
		setSize(width * 8 + 350, height * 16 + 175);
		
		// init file filters and file chooser
		fileFilters = new Filter[2];
		fileFilters[0] = new Filter("Image files (*.bin)", null, "bin", false);
		fileFilters[1] = new Filter("File raw_out.bin", "raw_out", "bin", true);
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.addChoosableFileFilter(fileFilters[0]);
		chooser.addChoosableFileFilter(fileFilters[1]);
		
		// set the location of the window to the center
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		DisplayMode dm = gd.getDisplayMode();
		int x = (dm.getWidth() - getWidth()) / 2;
		int y = (dm.getHeight() - getHeight()) / 2;
		setLocation(x, y);
		
		// init the options dialog
		dialog = new OptionDialog(this);
		dialog.init();
		// init the breakpoints dialog
		breakPoints = new BreakPoints(this);
		breakPoints.init();
		// init the statistics dialog
		stats = new Statistics(this);
		stats.init();
		setVisible(true);
	}
	
	/**
	 * Method to obtain a JButton initialized with a given label and an
	 * ActionListener
	 *
	 * @param text the label of the button
	 * @return an initialized instance
	 */
	private JButton createButton(String text, ActionListener listener, Dimension d)
	{
		JButton button;
		
		button = new JButton(text);
		button.setActionCommand(text);
		button.addActionListener(listener);
		if (d != null)
			button.setPreferredSize(d);
		return button;
	}
	
	private void updateButtons(int status)
	{
		if (status == MODE_START || status == MODE_STATISTIC || status == MODE_ERROR)
		{
			stopB.setEnabled(status != MODE_ERROR);
			singleOverB.setEnabled(false);
			singleIntoB.setEnabled(false);
			startB.setEnabled(false);
			optionsB.setEnabled(false);
			startB.setEnabled(false);
			statisticB.setEnabled(false);
			breakpointB.setEnabled(false);
		}
		else if (status == MODE_STOP)
		{
			stopB.setEnabled(false);
			singleOverB.setEnabled(true);
			singleIntoB.setEnabled(true);
			startB.setEnabled(true);
			optionsB.setEnabled(true);
			startB.setEnabled(true);
			statisticB.setEnabled(true);
			breakpointB.setEnabled(true);
		}
	}
	
	/**
	 * @see ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		boolean choice;
		// switch different buttons
		if (cmd.equals(B_QUIT))
			System.exit(0);
		else if (cmd.equals(B_LOAD))
		{
			// if file is selected
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				loadFile(chooser.getSelectedFile().getAbsolutePath());
				startB.setEnabled(true);
				singleIntoB.setEnabled(true);
				singleOverB.setEnabled(true);
				statisticB.setEnabled(true);
			}
		}
		else if ((choice = cmd.equals(B_START)) || cmd.equals(B_STATISTIC))
		{
			// set running mode and start thread
			runNormal = choice;
			runner = new Runner(runNormal);
			runner.start();
			if (choice)
				updateButtons(MODE_START);
			else
				updateButtons(MODE_STATISTIC);
		}
		else if ((choice = cmd.equals(B_SINGLE_INTO)) || cmd.equals(B_SINGLE_OVER))
		{
			// update the instruction and instruction pointer
			currIP = emulator.getCurrentIP();
			nextIns = nextIns.next;
			// reset the proceed flag
			proceed = false;
			if (!emulator.step(choice))
				updateButtons(MODE_ERROR);
			// if the instruction isn't in the current view
			if (nextIns == null || nextIns.startIP != currIP)
				updateMethodView();
			else
				disassWindow.step();
			// if the emulator signalized to continue and we are in a step over,
			// start the runner in the given mode
			if (!choice && proceed)
			{
				runner = new Runner(runNormal);
				runner.start();
			}
		}
		else if (cmd.equals(B_STOP))
		{
			// stop the runner
			runner.setRunning(false);
			runner = null;
			// reset the call condition
			emulator.stepOverC = null;
			updateButtons(MODE_STOP);
			// if we were in statistic mode, show the statistics
			if (!runNormal)
			{
				statisticB.setEnabled(true);
				stats.setVisible(true);
			}
		}
		else if (cmd.equals(B_OPTIONS))
		{
			dialog.setVisible(true);
		}
		else if (cmd.equals(B_BREAKPOINTS))
		{
			breakPoints.setVisible(true);
		}
		else if (cmd.equals(B_EXPORT))
		{
			try
			{
				PrintWriter writer = new PrintWriter(new FileOutputStream("disass.txt"));
				writer.write(disassWindow.getText());
				writer.flush();
				writer.close();
			}
			catch (IOException iox)
			{
				iox.printStackTrace();
			}
		}
	}
	
	/**
	 * @see MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2)
		{
			// determine the line where to set the condition
			int line = disassWindow.getLine(e.getPoint().y);
			if (line == -1)
				return;
			// determine the Mnemonic and the corresponding instruction pointer
			int cnt = 0;
			Mnemonic temp = currMethod.firstMnemo;
			while (cnt != line)
			{
				temp = temp.next;
				cnt++;
			}
			Condition c = new BreakPoint(temp.startIP);
			c.addToEmulator(emulator);
			JOptionPane.showMessageDialog(this, "Breakpoint set at 0x" + Integer.toHexString(temp.startIP).toUpperCase(), "Information", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	/**
	 * @see MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e)
	{
	}
	
	/**
	 * @see MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e)
	{
	}
	
	/**
	 * @see MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e)
	{
	}
	
	/**
	 * @see MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e)
	{
	}
	
	/**
	 * @see KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e)
	{
		//if (e.getKeyCode()==KeyEvent.VK_CONTROL) ctrl=true;
	}
	
	/**
	 * @see KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e)
	{
		//if (e.getKeyCode()==KeyEvent.VK_CONTROL) ctrl=false;
	}
	
	/**
	 * @see KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e)
	{
		//new JDialog(this, ""+nr, true).setVisible(true);
	}
	
	/**
	 * Method to update the view of the method in the method disassembly window
	 */
	private void updateMethodView()
	{
		currIP = emulator.getCurrentIP();
		// instruction not found in current method look in other already
		// parsed methods
		if (currIP < currMethod.firstIP || currIP > currMethod.lastIP)
		{
			currMethod = main.getMethod(currIP);
			// instruction not found in already parsed lists
			if (currMethod == null)
			{
				int methodStart = emulator.getStartOfMethod(currIP);
				// parse
				if ((currMethod = emulator.getMnemonicList(methodStart)) == null)
				{
					proceed = false;
					updateButtons(MODE_ERROR);
					tp.println("Error disassembling main method");
					return;
				}
				else
					main.add(currMethod);
			}
			loadMethod(currMethod);
		}
		// set highlight frame in list
		int cnt = 0;
		nextIns = currMethod.firstMnemo;
		while (nextIns != null && nextIns.startIP != currIP)
		{
			nextIns = nextIns.next;
			cnt++;
		}
		if (nextIns == null)
		{
			proceed = false;
			updateButtons(MODE_ERROR);
			tp.println("Could not find start of current instruction");
		}
		disassWindow.setSelectedLine(cnt);
	}
	
	/**
	 * Method to load a method into the JTextArea
	 *
	 * @param method the disassembled method
	 */
	private void loadMethod(MethodDisassembly method)
	{
		String jmpText;
		// clear disassembly window and load with data
		disassWindow.setText("");
		disassWindow.showSelection = true;
		disassWindow.curLine = 0;
		Mnemonic mn = method.firstMnemo;
		while (mn != null)
		{
			if (mn.compString == null)
			{
				if (mn.isJumpDest)
					jmpText = "+ ";
				else
					jmpText = "  ";
				mn.compString = Integer.toHexString(mn.startIP) + jmpText + mn.mnemo + " " + mn.parameters + "\n";
			}
			disassWindow.append(mn.compString);
			mn = mn.next;
		}
		disassWindow.append("method end, pointer to code points to " + Integer.toHexString(method.firstIP - emulator.codeStart) + "\n");
	}
	
	/**
	 * Method to load an image file into the emulator
	 *
	 * @param fName the name of the file to load
	 */
	private void loadFile(String fName)
	{
		byte[] data;
		
		tp = new LogOut();
		if ((data = readFile(fName)) != null)
		{
			// read the selected file
			emulator = EmulFactory.getEmulator("ssa");
			emulator.setBreakPointListener(this);
			// and init the given emulator with the data
			emulator.initFromRawOut(data, 32 * 1024 * 1024, tp);
			// register a BasicVGA for output
			BasicVGA vga = new BasicVGA(out);
			emulator.registerBlock(vga);
			// determine and set the main method
			if ((main = emulator.getMnemonicList(emulator.getCurrentIP())) == null)
			{
				emulator = null;
				tp.println("Error disassembling main method");
				return;
			}
			loadMethod(main);
			currMethod = main;
			// set instruction and current instruction pointer
			nextIns = main.firstMnemo;
			currIP = emulator.getCurrentIP();
			// create method statistics for the main method
			methStatMain = new MethodStatistics(currIP - emulator.codeStart, currIP, emulator.getEndOfMethod(currIP));
			methStatCur = methStatMain;
			// reset number of executed instructions
			numberOfExecIns = 0;
			// needed if startup with method load
			updateButtons(MODE_STOP);
			tp.println("File loaded");
		}
		else
			tp.println("Error opening file");
	}
	
	private byte[] readFile(String fname)
	{
		InputStream is;
		int cnt;
		byte[] data = null;
		
		is = null;
		try
		{
			is = new FileInputStream(fname);
			cnt = is.available();
			data = new byte[cnt];
			if (is.read(data, 0, cnt) != cnt)
			{
				is.close();
				return null;
			}
			is.close();
		}
		catch (IOException e)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * @see BreakPointListener#breakPointOccured(Condition)
	 */
	public void breakPointOccurred(Condition c)
	{
		// stop thread
		if (runner != null)
		{
			runner.setRunning(false);
			runner = null;
		}
		updateButtons(MODE_STOP);
	}
	
	/**
	 * @see BreakPointListener#proceedAfterStep()
	 */
	public void proceedAfterStep()
	{
		// init the runner as the Emulator has taken care, that a Stack breakpoint
		// has been set
		proceed = true;
	}
	
	/**
	 * @see BreakPointListener#endlessLoopDetected()
	 */
	public void endlessLoopDetected()
	{
		// stop the thread
		runner.setRunning(false);
		runner = null;
		// set the buttons
		updateButtons(MODE_ERROR);
		// inform the user
		tp.println("Endless loop detected");
		if (!runNormal)
			stats.setVisible(true);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Emulate emulateMe = new Emulate();
		if (args.length > 0)
			emulateMe.loadFile(args[0]);
	}
}
