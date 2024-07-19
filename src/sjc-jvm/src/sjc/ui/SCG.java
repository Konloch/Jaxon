/* Copyright (C) 2010, 2012 Stefan Frenz
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

import sjc.compbase.Context;
import sjc.osio.sun.ReflectionSymbols;
import sjc.osio.sun.SunOS;
import sjc.symbols.SymbolFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * SCG: manage a compilation with a GUI
 *
 * @author S. Frenz
 * @version 120305 removed serialVersionUID vars
 * version 101222 added option "do not encode sync-calls" (-R)
 * version 100513 removed simplified tab for Rainbow options, added support for multi-file-selection and status of current working directory
 * version 100504 added simplified tab for ATmega options
 * version 100429 added option for "interface parents array" (-j), removed "structs as objects" (-r)
 * version 100424 removed options for "return missing" (-R), added check of screen size
 * version 100422 initial version
 */

public class SCG extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	public static void main(String[] argv)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new SCG();
			}
		});
	}
	
	private static class LabelComp extends JPanel
	{
		private static final long serialVersionUID = 1L;
		
		public LabelComp(String text, JComponent comp)
		{
			setLayout(new BorderLayout(2, 0));
			add(new JLabel(text), BorderLayout.WEST);
			add(comp, BorderLayout.CENTER);
		}
	}
	
	private static class ThreeComp extends JPanel
	{
		private static final long serialVersionUID = 1L;
		
		public ThreeComp(JComponent center, JComponent southWest, JComponent southEast)
		{
			setLayout(new BorderLayout(2, 0));
			add(center, BorderLayout.CENTER);
			JPanel south = new JPanel(new GridLayout(1, 1, 2, 0));
			south.add(southWest);
			south.add(southEast);
			add(south, BorderLayout.SOUTH);
		}
	}
	
	private final JTextField result;
	private final JButton run;
	private final JFileChooser fileChooser;
	private final Vector<String> args;
	
	//preselect / expert pane
	private final JTabbedPane preselect;
	
	//expert options
	private final JCheckBox v_verbose;
	private final JCheckBox V_verboseTiming;
	private final JCheckBox L_flat;
	private final JCheckBox l_streamline;
	private final JCheckBox M_indirect;
	private final JCheckBox m_dynaMem;
	private final JTextField i_options;
	private final JTextField H_listFiles;
	private final JCheckBox h_heapAssign;
	private final JCheckBox c_allAssign;
	private final JCheckBox w_alternateNew;
	private final JCheckBox x_stackExtreme;
	private final JCheckBox C_skipArrayCheck;
	private final JCheckBox B_skipBoundCheck;
	private final JCheckBox b_rteBound;
	private final JCheckBox n_rteNull;
	private final JCheckBox X_explicitConv;
	private final JCheckBox d_mthdDebug;
	private final JCheckBox g_genAllUnits;
	private final JCheckBox G_genAllMthds;
	private final JCheckBox j_genIntfParents;
	private final JCheckBox q_relManager;
	private final JCheckBox Q_srcHint;
	private final JCheckBox W_printCode;
	private final JCheckBox F_profiler;
	private final JCheckBox f_throwWithoutCatch;
	private final JCheckBox R_skipSync;
	private final JCheckBox y_byteString;
	private final JCheckBox k_skipInlineMthd;
	private final JCheckBox Y_nativeFloatInComp;
	private final JTextField N_startMthd;
	private final JTextField I_maxInlLev;
	private final JTextField S_autoInlStmts;
	private final JTextField K_allocMask;
	private final JTextField s_imgSize;
	private final JComboBox t_arch;
	private final JTextField T_archOpt;
	private final JTextField a_addr;
	private final JTextField Z_reloc;
	private final JTextField e_embAddr;
	private final JCheckBox E_constInRam;
	private final JTextField P_specialHeader;
	private final JTextField p_namePrefix;
	private final JComboBox o_out;
	private final JTextField O_outPar;
	private final JComboBox u_sym;
	private final JTextField U_symPar;
	private final JTextField D_code;
	private final JTextField D_gcc;
	private final JTextField D_mthd;
	private final JTextField D_sym;
	private final JTextField D_rel;
	
	//simplified ATmega options
	private final JRadioButton sAT_m8;
	private final JRadioButton sAT_m16;
	private final JRadioButton sAT_m32;
	private final JRadioButton sAT_m64;
	private final JRadioButton sAT_skipBoundCheck;
	private final JRadioButton sAT_rteBound;
	private final JCheckBox sAT_skipArrayCheck;
	private final JCheckBox sAT_explicitConv;
	private final JTextField sAT_headerFile;
	private final JTextField sAT_bootConf;
	private final JCheckBox sAT_skipInlineMthd;
	private final JCheckBox sAT_byteString;
	private final JCheckBox sAT_optimEnabled;
	private final JButton sAT_initDefault;
	
	//files and directories to compile
	private final JList files;
	private final JList dirs;
	private final JList dirsNonRec;
	private final DefaultListModel filesList;
	private final DefaultListModel dirsList;
	private final DefaultListModel dirsNonRecList;
	private final JButton fileAdd;
	private final JButton fileDel;
	private final JButton dirAdd;
	private final JButton dirDel;
	private final JButton dirNRAdd;
	private final JButton dirNRDel;
	private final JTextField workDir;
	
	private final ActionListener sAT_OptionChangedListener = new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			triggerATmegaEvaluation(e.getSource());
		}
	};
	private final ActionListener expertOptionChangedListener = new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			triggerExpertEvaluation(e.getSource());
		}
	};
	private final DocumentListener expertTextChangedListener = new DocumentListener()
	{
		public void removeUpdate(DocumentEvent e)
		{
			triggerExpertEvaluation(e.getDocument());
		}
		
		public void insertUpdate(DocumentEvent e)
		{
			triggerExpertEvaluation(e.getDocument());
		}
		
		public void changedUpdate(DocumentEvent e)
		{
		}
	};
	private final ActionListener butAddDelListener = new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			triggerAddDel(e.getSource());
		}
	};
	private final ActionListener runListener = new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			doCompile();
		}
	};
	
	private SCG()
	{
		super("smfCompiler option selector GUI");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		JPanel tmpPanel;
		ButtonGroup bg;
		
		//preselection pane
		add(preselect = new JTabbedPane(), BorderLayout.NORTH);
		
		//expert tab
		tmpPanel = new JPanel();
		tmpPanel.setLayout(new GridLayout(0, 4, 2, 2));
		tmpPanel.add(L_flat = new JCheckBox("flat memory"));
		L_flat.addActionListener(expertOptionChangedListener);
		tmpPanel.add(l_streamline = new JCheckBox("streamline objects"));
		l_streamline.addActionListener(expertOptionChangedListener);
		tmpPanel.add(M_indirect = new JCheckBox("indirect scalars"));
		M_indirect.addActionListener(expertOptionChangedListener);
		tmpPanel.add(m_dynaMem = new JCheckBox("movable objects"));
		m_dynaMem.addActionListener(expertOptionChangedListener);
		tmpPanel.add(v_verbose = new JCheckBox("verbose"));
		v_verbose.addActionListener(expertOptionChangedListener);
		tmpPanel.add(V_verboseTiming = new JCheckBox("verbose timing"));
		V_verboseTiming.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("options from file", i_options = new JTextField()));
		i_options.getDocument().addDocumentListener(expertTextChangedListener);
		tmpPanel.add(new LabelComp("log compiled files to file", H_listFiles = new JTextField()));
		H_listFiles.addActionListener(expertOptionChangedListener);
		tmpPanel.add(h_heapAssign = new JCheckBox("rte-call for heap pointer assignments"));
		h_heapAssign.addActionListener(expertOptionChangedListener);
		tmpPanel.add(c_allAssign = new JCheckBox("rte-call for all pointer assignments"));
		c_allAssign.addActionListener(expertOptionChangedListener);
		tmpPanel.add(w_alternateNew = new JCheckBox("alternate object layout"));
		w_alternateNew.addActionListener(expertOptionChangedListener);
		tmpPanel.add(x_stackExtreme = new JCheckBox("stack extreme check"));
		x_stackExtreme.addActionListener(expertOptionChangedListener);
		tmpPanel.add(C_skipArrayCheck = new JCheckBox("skip array check"));
		C_skipArrayCheck.addActionListener(expertOptionChangedListener);
		tmpPanel.add(B_skipBoundCheck = new JCheckBox("skip bound check"));
		B_skipBoundCheck.addActionListener(expertOptionChangedListener);
		tmpPanel.add(b_rteBound = new JCheckBox("rte-call for bound exception"));
		b_rteBound.addActionListener(expertOptionChangedListener);
		tmpPanel.add(n_rteNull = new JCheckBox("rte-call for null exception"));
		n_rteNull.addActionListener(expertOptionChangedListener);
		tmpPanel.add(X_explicitConv = new JCheckBox("require explicit conversion"));
		X_explicitConv.addActionListener(expertOptionChangedListener);
		tmpPanel.add(d_mthdDebug = new JCheckBox("create method binary debug files"));
		d_mthdDebug.addActionListener(expertOptionChangedListener);
		tmpPanel.add(g_genAllUnits = new JCheckBox("create all units (don't skip)"));
		g_genAllUnits.addActionListener(expertOptionChangedListener);
		tmpPanel.add(G_genAllMthds = new JCheckBox("create all methods (don't skip)"));
		G_genAllMthds.addActionListener(expertOptionChangedListener);
		tmpPanel.add(j_genIntfParents = new JCheckBox("generate interface parents array"));
		j_genIntfParents.addActionListener(expertOptionChangedListener);
		tmpPanel.add(q_relManager = new JCheckBox("enable relation manager"));
		q_relManager.addActionListener(expertOptionChangedListener);
		tmpPanel.add(Q_srcHint = new JCheckBox("integrate source hints"));
		Q_srcHint.addActionListener(expertOptionChangedListener);
		tmpPanel.add(W_printCode = new JCheckBox("print code of methods"));
		W_printCode.addActionListener(expertOptionChangedListener);
		tmpPanel.add(F_profiler = new JCheckBox("enable profiler call for every method"));
		F_profiler.addActionListener(expertOptionChangedListener);
		tmpPanel.add(f_throwWithoutCatch = new JCheckBox("disable throw-frames (no catch)"));
		f_throwWithoutCatch.addActionListener(expertOptionChangedListener);
		tmpPanel.add(R_skipSync = new JCheckBox("do not encode rte-calls for synchronized"));
		R_skipSync.addActionListener(expertOptionChangedListener);
		tmpPanel.add(y_byteString = new JCheckBox("Strings with byte-value (not char)"));
		y_byteString.addActionListener(expertOptionChangedListener);
		tmpPanel.add(k_skipInlineMthd = new JCheckBox("skip method objects for inline methods"));
		k_skipInlineMthd.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("start code in method", N_startMthd = new JTextField()));
		N_startMthd.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("maximum inline level", I_maxInlLev = new JTextField()));
		I_maxInlLev.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("max. stmt. count for auto-inline", S_autoInlStmts = new JTextField()));
		S_autoInlStmts.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("align-mask for allocated block", K_allocMask = new JTextField()));
		K_allocMask.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("memory image size", s_imgSize = new JTextField()));
		s_imgSize.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("architecture", t_arch = new JComboBox(new Object[]{"ia32", "ia32opt", "amd64", "atmega", "atmegaOpt", "ssa32", "ssa64"})));
		t_arch.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("architecture options", T_archOpt = new JTextField()));
		T_archOpt.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("address", a_addr = new JTextField()));
		a_addr.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("relocate by GB", Z_reloc = new JTextField()));
		Z_reloc.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("embedded RAM address", e_embAddr = new JTextField()));
		e_embAddr.addActionListener(expertOptionChangedListener);
		tmpPanel.add(E_constInRam = new JCheckBox("constant objects in RAM"));
		E_constInRam.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("image header (or \"none\")", P_specialHeader = new JTextField()));
		P_specialHeader.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("output name prefix", p_namePrefix = new JTextField()));
		p_namePrefix.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("output format", o_out = new JComboBox(new Object[]{"raw", "boot"})));
		o_out.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("output options", O_outPar = new JTextField()));
		O_outPar.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("in-system symbol generator", u_sym = new JComboBox(new Object[]{"none", "raw", "rte", "mthd", "reflect"})));
		u_sym.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("symbol generator options", U_symPar = new JTextField()));
		U_symPar.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("debug writer \"code\" file", D_code = new JTextField()));
		D_code.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("debug writer \"gcc\" file", D_gcc = new JTextField()));
		D_gcc.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("debug writer \"mthd\" file", D_mthd = new JTextField()));
		D_mthd.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("debug writer \"sym\" file", D_sym = new JTextField()));
		D_sym.addActionListener(expertOptionChangedListener);
		tmpPanel.add(new LabelComp("debug writer \"rel\" file", D_rel = new JTextField()));
		D_rel.addActionListener(expertOptionChangedListener);
		preselect.addTab("expert", tmpPanel);
		tmpPanel.add(Y_nativeFloatInComp = new JCheckBox("prefer native float code in compiler"));
		Y_nativeFloatInComp.addActionListener(expertOptionChangedListener);
		
		//ATmega tab
		tmpPanel = new JPanel();
		tmpPanel.setLayout(new GridLayout(0, 4, 2, 2));
		bg = new ButtonGroup();
		tmpPanel.add(sAT_m8 = new JRadioButton("8k flash"));
		sAT_m8.addActionListener(sAT_OptionChangedListener);
		bg.add(sAT_m8);
		tmpPanel.add(sAT_m16 = new JRadioButton("16k flash"));
		sAT_m16.addActionListener(sAT_OptionChangedListener);
		bg.add(sAT_m16);
		tmpPanel.add(sAT_m32 = new JRadioButton("32k flash"));
		sAT_m32.addActionListener(sAT_OptionChangedListener);
		bg.add(sAT_m32);
		tmpPanel.add(sAT_m64 = new JRadioButton("64k flash"));
		sAT_m64.addActionListener(sAT_OptionChangedListener);
		bg.add(sAT_m64);
		bg = new ButtonGroup();
		tmpPanel.add(sAT_rteBound = new JRadioButton("rte array bound check"));
		sAT_rteBound.addActionListener(sAT_OptionChangedListener);
		bg.add(sAT_rteBound);
		tmpPanel.add(sAT_skipBoundCheck = new JRadioButton("skip array bound check"));
		sAT_skipBoundCheck.addActionListener(sAT_OptionChangedListener);
		bg.add(sAT_skipBoundCheck);
		tmpPanel.add(sAT_skipArrayCheck = new JCheckBox("skip array store check"));
		sAT_skipArrayCheck.addActionListener(sAT_OptionChangedListener);
		tmpPanel.add(sAT_explicitConv = new JCheckBox("require explicit conversion"));
		sAT_explicitConv.addActionListener(sAT_OptionChangedListener);
		tmpPanel.add(new LabelComp("header file", sAT_headerFile = new JTextField()));
		sAT_headerFile.addActionListener(sAT_OptionChangedListener);
		tmpPanel.add(new LabelComp("boot configuration", sAT_bootConf = new JTextField()));
		sAT_bootConf.addActionListener(sAT_OptionChangedListener);
		tmpPanel.add(sAT_skipInlineMthd = new JCheckBox("skip method objects for inline methods"));
		sAT_skipInlineMthd.addActionListener(sAT_OptionChangedListener);
		tmpPanel.add(sAT_byteString = new JCheckBox("Strings with byte-value (not char)"));
		sAT_byteString.addActionListener(sAT_OptionChangedListener);
		tmpPanel.add(sAT_optimEnabled = new JCheckBox("enable code optimizer"));
		sAT_optimEnabled.addActionListener(sAT_OptionChangedListener);
		tmpPanel.add(sAT_initDefault = new JButton("init defaults for selected chip"));
		sAT_initDefault.addActionListener(sAT_OptionChangedListener);
		preselect.addTab("ATmega", tmpPanel);
		
		//south panel
		tmpPanel = new JPanel();
		tmpPanel.setLayout(new GridLayout(0, 4, 2, 2));
		tmpPanel.add(new ThreeComp(new LabelComp("compile files", new JScrollPane(files = new JList(filesList = new DefaultListModel()))), fileAdd = new JButton("add"), fileDel = new JButton("del")));
		filesList.addElement("a");
		filesList.addElement("a");
		filesList.addElement("a");
		filesList.addElement("a");
		filesList.addElement("a");//insert dummy entries for later pack-call
		files.setVisibleRowCount(5);
		fileAdd.addActionListener(butAddDelListener);
		fileDel.addActionListener(butAddDelListener);
		tmpPanel.add(new ThreeComp(new LabelComp("directories", new JScrollPane(dirs = new JList(dirsList = new DefaultListModel()))), dirAdd = new JButton("add"), dirDel = new JButton("del")));
		dirs.setVisibleRowCount(5);
		dirAdd.addActionListener(butAddDelListener);
		dirDel.addActionListener(butAddDelListener);
		tmpPanel.add(new ThreeComp(new LabelComp("dir. non-rec.", new JScrollPane(dirsNonRec = new JList(dirsNonRecList = new DefaultListModel()))), dirNRAdd = new JButton("add"), dirNRDel = new JButton("del")));
		dirsNonRec.setVisibleRowCount(5);
		dirNRAdd.addActionListener(butAddDelListener);
		dirNRDel.addActionListener(butAddDelListener);
		JPanel tmpTmpPanel = new JPanel();
		tmpTmpPanel.setLayout(new GridLayout(0, 1, 2, 2));
		tmpTmpPanel.add(new LabelComp("work.dir.", workDir = new JTextField(System.getProperty("user.dir"))));
		workDir.setEditable(false);
		tmpTmpPanel.add(run = new JButton("run"));
		run.addActionListener(runListener);
		tmpPanel.add(tmpTmpPanel);
		add(tmpPanel, BorderLayout.CENTER);
		add(new LabelComp("command line:", result = new JTextField()), BorderLayout.SOUTH);
		result.setEditable(false);
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int width = getWidth();
		int height = getHeight();
		if (width > screen.getWidth())
			width = (int) screen.getWidth();
		if (height > screen.getHeight())
			height = (int) screen.getHeight();
		setSize(width, height);
		filesList.removeAllElements(); //clear dummy list
		setVisible(true);
		fileChooser = new JFileChooser();
		args = new Vector<String>();
	}
	
	private void appendOption(StringBuffer line, String option)
	{
		if (line.length() > 0)
			line.append(' ');
		line.append(option);
		args.add(option);
	}
	
	private void appendOption(StringBuffer line, String opt1, String opt2)
	{
		if (line.length() > 0)
			line.append(' ');
		line.append(opt1);
		line.append(' ');
		line.append(opt2);
		args.add(opt1);
		args.add(opt2);
	}
	
	private void appendOption(StringBuffer line, String opt1, String opt2, String opt3)
	{
		if (line.length() > 0)
			line.append(' ');
		line.append(opt1);
		line.append(' ');
		line.append(opt2);
		line.append(' ');
		line.append(opt3);
		args.add(opt1);
		args.add(opt2);
		args.add(opt3);
	}
	
	private void triggerATmegaEvaluation(Object source)
	{
		int size = 0;
		if (sAT_m8.isSelected())
			size = 8;
		else if (sAT_m16.isSelected())
			size = 16;
		else if (sAT_m32.isSelected())
			size = 32;
		else if (sAT_m64.isSelected())
			size = 64;
		if (size == 0)
		{
			sAT_m32.setSelected(true);
			size = 32;
		}
		if (source == sAT_initDefault)
		{
			sAT_skipBoundCheck.setSelected(true);
			sAT_skipArrayCheck.setSelected(true);
			sAT_explicitConv.setSelected(true);
			sAT_optimEnabled.setSelected(false);
			sAT_headerFile.setText("batmel" + size + ".bin");
			sAT_skipInlineMthd.setSelected(true);
			sAT_byteString.setSelected(true);
		}
		resetAllExpertValues();
		L_flat.setSelected(true);
		e_embAddr.setText("0x60");
		E_constInRam.setSelected(true);
		a_addr.setText("0");
		s_imgSize.setText(size + "k");
		if (sAT_skipBoundCheck.isSelected())
			B_skipBoundCheck.setSelected(true);
		else
			b_rteBound.setSelected(true);
		if (sAT_skipArrayCheck.isSelected())
			C_skipArrayCheck.setSelected(true);
		if (sAT_explicitConv.isSelected())
			X_explicitConv.setSelected(true);
		if (sAT_optimEnabled.isSelected())
			t_arch.setSelectedItem("atmegaOpt");
		else
			t_arch.setSelectedItem("atmega");
		if (sAT_skipInlineMthd.isSelected())
			k_skipInlineMthd.setSelected(true);
		if (sAT_byteString.isSelected())
			y_byteString.setSelected(true);
		P_specialHeader.setText(sAT_headerFile.getText());
		o_out.setSelectedItem("boot");
		O_outPar.setText(sAT_bootConf.getText());
		triggerExpertEvaluation(null);
	}
	
	private void resetAllExpertValues()
	{
		v_verbose.setSelected(false);
		V_verboseTiming.setSelected(false);
		L_flat.setSelected(false);
		l_streamline.setSelected(false);
		M_indirect.setSelected(false);
		m_dynaMem.setSelected(false);
		i_options.setText("");
		H_listFiles.setText("");
		h_heapAssign.setSelected(false);
		c_allAssign.setSelected(false);
		w_alternateNew.setSelected(false);
		x_stackExtreme.setSelected(false);
		C_skipArrayCheck.setSelected(false);
		B_skipBoundCheck.setSelected(false);
		b_rteBound.setSelected(false);
		n_rteNull.setSelected(false);
		X_explicitConv.setSelected(false);
		d_mthdDebug.setSelected(false);
		g_genAllUnits.setSelected(false);
		G_genAllMthds.setSelected(false);
		j_genIntfParents.setSelected(false);
		q_relManager.setSelected(false);
		Q_srcHint.setSelected(false);
		W_printCode.setSelected(false);
		F_profiler.setSelected(false);
		f_throwWithoutCatch.setSelected(false);
		R_skipSync.setSelected(false);
		y_byteString.setSelected(false);
		k_skipInlineMthd.setSelected(false);
		Y_nativeFloatInComp.setSelected(false);
		N_startMthd.setText("");
		I_maxInlLev.setText("");
		S_autoInlStmts.setText("");
		K_allocMask.setText("");
		s_imgSize.setText("");
		t_arch.setSelectedIndex(0);
		T_archOpt.setText("");
		a_addr.setText("");
		Z_reloc.setText("");
		e_embAddr.setText("");
		E_constInRam.setSelected(false);
		P_specialHeader.setText("");
		p_namePrefix.setText("");
		o_out.setSelectedIndex(0);
		O_outPar.setText("");
		u_sym.setSelectedIndex(0);
		U_symPar.setText("");
		D_code.setText("");
		D_gcc.setText("");
		D_mthd.setText("");
		D_sym.setText("");
		D_rel.setText("");
	}
	
	private void triggerExpertEvaluation(Object source)
	{
		//check for invalid combinations
		if (source == L_flat || source == l_streamline || source == e_embAddr || source == E_constInRam)
		{
			M_indirect.setSelected(false);
			m_dynaMem.setSelected(false);
		}
		else if (source == M_indirect || source == m_dynaMem)
		{
			L_flat.setSelected(false);
			l_streamline.setSelected(false);
			e_embAddr.setText("");
			E_constInRam.setSelected(false);
		}
		else if (source == h_heapAssign)
			c_allAssign.setSelected(false);
		else if (source == c_allAssign)
			h_heapAssign.setSelected(false);
		else if (source == B_skipBoundCheck)
			b_rteBound.setSelected(false);
		else if (source == b_rteBound)
			B_skipBoundCheck.setSelected(false);
		//build new command line
		args.clear();
		StringBuffer line = new StringBuffer();
		String text;
		if (v_verbose.isSelected())
			appendOption(line, "-v");
		if (V_verboseTiming.isSelected())
			appendOption(line, "-V");
		if (L_flat.isSelected())
			appendOption(line, "-L");
		if (l_streamline.isSelected())
			appendOption(line, "-l");
		if (M_indirect.isSelected())
			appendOption(line, "-M");
		if (m_dynaMem.isSelected())
			appendOption(line, "-m");
		if ((text = i_options.getText()).length() > 0)
			appendOption(line, "-i", text);
		if ((text = H_listFiles.getText()).length() > 0)
			appendOption(line, "-H", text);
		if (h_heapAssign.isSelected())
			appendOption(line, "-h");
		if (c_allAssign.isSelected())
			appendOption(line, "-c");
		if (w_alternateNew.isSelected())
			appendOption(line, "-w");
		if (x_stackExtreme.isSelected())
			appendOption(line, "-x");
		if (C_skipArrayCheck.isSelected())
			appendOption(line, "-C");
		if (B_skipBoundCheck.isSelected())
			appendOption(line, "-B");
		if (b_rteBound.isSelected())
			appendOption(line, "-b");
		if (n_rteNull.isSelected())
			appendOption(line, "-n");
		if (X_explicitConv.isSelected())
			appendOption(line, "-X");
		if (d_mthdDebug.isSelected())
			appendOption(line, "-n");
		if (g_genAllUnits.isSelected())
			appendOption(line, "-g");
		if (G_genAllMthds.isSelected())
			appendOption(line, "-G");
		if (j_genIntfParents.isSelected())
			appendOption(line, "-j");
		if (q_relManager.isSelected())
			appendOption(line, "-q");
		if (Q_srcHint.isSelected())
			appendOption(line, "-Q");
		if (W_printCode.isSelected())
			appendOption(line, "-W");
		if (F_profiler.isSelected())
			appendOption(line, "-F");
		if (f_throwWithoutCatch.isSelected())
			appendOption(line, "-f");
		if (R_skipSync.isSelected())
			appendOption(line, "-R");
		if (y_byteString.isSelected())
			appendOption(line, "-y");
		if (k_skipInlineMthd.isSelected())
			appendOption(line, "-k");
		if (Y_nativeFloatInComp.isSelected())
			appendOption(line, "-Y");
		if ((text = N_startMthd.getText()).length() > 0)
			appendOption(line, "-N", text);
		if ((text = I_maxInlLev.getText()).length() > 0)
			appendOption(line, "-I", text);
		if ((text = S_autoInlStmts.getText()).length() > 0)
			appendOption(line, "-S", text);
		if ((text = K_allocMask.getText()).length() > 0)
			appendOption(line, "-K", text);
		if ((text = s_imgSize.getText()).length() > 0)
			appendOption(line, "-s", text);
		if (!(text = (String) t_arch.getSelectedItem()).equals("ia32"))
			appendOption(line, "-t", text);
		if ((text = T_archOpt.getText()).length() > 0)
			appendOption(line, "-T", text);
		if ((text = a_addr.getText()).length() > 0)
			appendOption(line, "-a", text);
		if ((text = Z_reloc.getText()).length() > 0)
			appendOption(line, "-Z", text);
		if ((text = e_embAddr.getText()).length() > 0)
			appendOption(line, "-e", text);
		if (E_constInRam.isSelected())
			appendOption(line, "-E");
		if ((text = P_specialHeader.getText()).length() > 0)
			appendOption(line, "-P", text);
		if ((text = p_namePrefix.getText()).length() > 0)
			appendOption(line, "-p", text);
		if (!(text = (String) o_out.getSelectedItem()).equals("raw"))
			appendOption(line, "-o", text);
		if ((text = O_outPar.getText()).length() > 0)
			appendOption(line, "-O", text);
		if (!(text = (String) u_sym.getSelectedItem()).equals("none"))
			appendOption(line, "-u", text);
		if ((text = U_symPar.getText()).length() > 0)
			appendOption(line, "-U", text);
		if ((text = D_code.getText()).length() > 0)
			appendOption(line, "-D", "code", text);
		if ((text = D_gcc.getText()).length() > 0)
			appendOption(line, "-D", "gcc", text);
		if ((text = D_mthd.getText()).length() > 0)
			appendOption(line, "-D", "mthd", text);
		if ((text = D_sym.getText()).length() > 0)
			appendOption(line, "-D", "sym", text);
		if ((text = D_rel.getText()).length() > 0)
			appendOption(line, "-D", "rel", text);
		Enumeration<?> elements = filesList.elements();
		while (elements.hasMoreElements())
			appendOption(line, (String) elements.nextElement());
		elements = dirsList.elements();
		while (elements.hasMoreElements())
			appendOption(line, (String) elements.nextElement());
		elements = dirsNonRecList.elements();
		while (elements.hasMoreElements())
			appendOption(line, elements.nextElement() + ":");
		result.setText(line.toString());
	}
	
	private void tryDelItems(JList list)
	{
		DefaultListModel model = (DefaultListModel) list.getModel();
		for (Object o : list.getSelectedValues())
			model.removeElement(o);
	}
	
	private void tryAddItem(JList list, boolean filesNotDirs)
	{
		if (filesNotDirs)
		{
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setMultiSelectionEnabled(true);
		}
		else
		{
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setMultiSelectionEnabled(false);
		}
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			DefaultListModel model = (DefaultListModel) list.getModel();
			for (File f : fileChooser.getSelectedFiles())
				model.addElement(f.getAbsolutePath());
		}
	}
	
	private void triggerAddDel(Object source)
	{
		if (source == fileAdd)
			tryAddItem(files, true);
		else if (source == fileDel)
			tryDelItems(files);
		else if (source == dirAdd)
			tryAddItem(dirs, false);
		else if (source == dirDel)
			tryDelItems(dirs);
		else if (source == dirNRAdd)
			tryAddItem(dirsNonRec, false);
		else if (source == dirNRDel)
			tryDelItems(dirsNonRec);
		else
		{
			JOptionPane.showMessageDialog(this, "internal SCG error");
			return;
		}
		triggerExpertEvaluation(source);
	}
	
	private void doCompile()
	{
		setEnabled(false);
		final String[] argv = new String[args.size()];
		final String argString = result.getText();
		args.copyInto(argv);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final JTextArea text;
				JFrame myWindow = new JFrame("SJC output");
				myWindow.add(new JScrollPane(text = new JTextArea()));
				text.append("calling SJC with parameters:\n");
				text.append(argString + "\n\n");
				text.setWrapStyleWord(false);
				text.setEditable(false);
				myWindow.setSize(400, 500);
				myWindow.setVisible(true);
				OutputStream textOut = new OutputStream()
				{
					public void write(int b) throws IOException
					{
						text.append(Character.toString((char) b));
					}
				};
				int res;
				Context ctx = new Context(new SunOS(textOut));
				SymbolFactory.preparedReflectionSymbols = new ReflectionSymbols();
				if ((res = ctx.compile(argv, "vJRE")) == 0)
					ctx.writeSymInfo();
				else
					ctx.out.println("\ncompiler result: " + res);
			}
		});
		setEnabled(true);
	}
}
