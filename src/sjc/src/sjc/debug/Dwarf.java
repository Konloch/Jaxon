/* Copyright (C) 2013, 2014, 2015 Stefan Frenz
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

package sjc.debug;

import sjc.compbase.*;
import sjc.osio.BinWriter;
import sjc.osio.TextBuffer;

/**
 * Dwarf: dwarf-debug info
 *
 * @author S. Frenz, T. Schmitt
 * @version 151026 fixed type conversion
 * version 140426 removed non-working variables (reset to 130406)
 * version 130508 added support for static non-final variables
 * version 130406 fixed line numbers, now using full qualified unit names, added base types
 * version 130403 fixed compilation unit end, removed unneeded sections, added source line information (not working yet)
 * version 130331 removed class descriptor from arange section (gdb is still not accepting the file)
 * version 130329 initial version
 */

//--- for ELF:
//see http://geezer.osdevbrasil.net/osd/exec/elf.txt
//and http://www.skyfree.org/linux/assembly/section5.html
//and http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
//get help from "readelf -a FILE"
//--- for DWARF:
//see www.dwarfstd.org
//see http://blog.techveda.org/index.php/howsourcedebuggerswork/
//get help from "dwarfdump FILE"

public class Dwarf extends DebugWriter
{
	//some infos about our file
	public final static int SECTION_COUNT = 8;
	public final static int SECTION_HEADER_SIZE = 40;
	public final static int DATASTART_IN_FILE = 52 + SECTION_COUNT * SECTION_HEADER_SIZE;
	
	//our sections
	public final static int SEC_NULL = 0;
	public final static int SEC_SHSTRTAB = 1;
	public final static int SEC_DEB_ARANGES = 2;
	public final static int SEC_DEB_INFO = 3;
	public final static int SEC_DEB_ABBREV = 4;
	public final static int SEC_DEB_LINE = 5;
	public final static int SEC_DEB_STR = 6;
	public final static int SEC_DEB_LOC = 7;
	
	//linux section types
	public final static int SHT_NULL = 0;
	public final static int SHT_PROGBITS = 1; //.text, .data, .bss etc.
	public final static int SHT_SYMTAB = 2;
	public final static int SHT_STRTAB = 3;
	
	//DWARF constants, see section 7 of defining document at www.darfstd.org
	public static final int DW_TAG_padding = 0x00;
	public static final int DW_TAG_array_type = 0x01;
	public static final int DW_TAG_class_type = 0x02;
	public static final int DW_TAG_entry_point = 0x03;
	public static final int DW_TAG_enumeration_type = 0x04;
	public static final int DW_TAG_formal_parameter = 0x05;
	public static final int DW_TAG_global_subroutine = 0x06;
	public static final int DW_TAG_global_variable = 0x07;
	public static final int DW_TAG_label = 0x0a;
	public static final int DW_TAG_lexical_block = 0x0b;
	public static final int DW_TAG_local_variable = 0x0c;
	public static final int DW_TAG_member = 0x0d;
	public static final int DW_TAG_pointer_type = 0x0f;
	public static final int DW_TAG_reference_type = 0x10;
	public static final int DW_TAG_compile_unit = 0x11;
	public static final int DW_TAG_string_type = 0x12;
	public static final int DW_TAG_structure_type = 0x13;
	public static final int DW_TAG_subroutine = 0x14;
	public static final int DW_TAG_subroutine_type = 0x15;
	public static final int DW_TAG_typedef = 0x16;
	public static final int DW_TAG_union_type = 0x17;
	public static final int DW_TAG_unspecified_parameters = 0x18;
	public static final int DW_TAG_variant = 0x19;
	public static final int DW_TAG_common_block = 0x1a;
	public static final int DW_TAG_common_inclusion = 0x1b;
	public static final int DW_TAG_inheritance = 0x1c;
	public static final int DW_TAG_inlined_subroutine = 0x1d;
	public static final int DW_TAG_module = 0x1e;
	public static final int DW_TAG_ptr_to_member_type = 0x1f;
	public static final int DW_TAG_set_type = 0x20;
	public static final int DW_TAG_subrange_type = 0x21;
	public static final int DW_TAG_with_stmt = 0x22;
	public static final int DW_TAG_access_declaration = 0x23;
	public static final int DW_TAG_base_type = 0x24;
	public static final int DW_TAG_catch_block = 0x25;
	public static final int DW_TAG_const_type = 0x26;
	public static final int DW_TAG_constant = 0x27;
	public static final int DW_TAG_enumerator = 0x28;
	public static final int DW_TAG_file_type = 0x29;
	public static final int DW_TAG_friend = 0x2a;
	public static final int DW_TAG_namelist = 0x2b;
	public static final int DW_TAG_namelist_item = 0x2c;
	public static final int DW_TAG_packed_type = 0x2d;
	public static final int DW_TAG_subprogram = 0x2e;
	public static final int DW_TAG_template_type_parameter = 0x2f;
	public static final int DW_TAG_template_value_parameter = 0x30;
	public static final int DW_TAG_thrown_type = 0x31;
	public static final int DW_TAG_try_block = 0x32;
	public static final int DW_TAG_variant_part = 0x33;
	public static final int DW_TAG_variable = 0x34;
	public static final int DW_TAG_volatile_type = 0x35;
	public static final int DW_TAG_dwarf_procedure = 0x36;
	public static final int DW_TAG_restrict_type = 0x37;
	public static final int DW_TAG_interface_type = 0x38;
	public static final int DW_TAG_namespace = 0x39;
	public static final int DW_TAG_imported_module = 0x3a;
	public static final int DW_TAG_unspecified_type = 0x3b;
	public static final int DW_TAG_partial_unit = 0x3c;
	public static final int DW_TAG_imported_unit = 0x3d;
	public static final int DW_TAG_condition = 0x3f;
	public static final int DW_TAG_shared_type = 0x40;
	
	public static final int DW_TAG_lo_user = 0x4080;
	public static final int DW_TAG_hi_user = 0xffff;
	
	public static final int DW_CHILDREN_no = 0x00;
	public static final int DW_CHILDREN_yes = 0x01;
	
	public static final int DW_FORM_addr = 0x01;
	public static final int DW_FORM_block2 = 0x03;
	public static final int DW_FORM_block4 = 0x04;
	public static final int DW_FORM_data2 = 0x05;
	public static final int DW_FORM_data4 = 0x06;
	public static final int DW_FORM_data8 = 0x07;
	public static final int DW_FORM_string = 0x08;
	public static final int DW_FORM_block = 0x09;
	public static final int DW_FORM_block1 = 0x0a;
	public static final int DW_FORM_data1 = 0x0b;
	public static final int DW_FORM_flag = 0x0c;
	public static final int DW_FORM_sdata = 0x0d;
	public static final int DW_FORM_strp = 0x0e;
	public static final int DW_FORM_udata = 0x0f;
	public static final int DW_FORM_ref_addr = 0x10;
	public static final int DW_FORM_ref1 = 0x11;
	public static final int DW_FORM_ref2 = 0x12;
	public static final int DW_FORM_ref4 = 0x13;
	public static final int DW_FORM_ref8 = 0x14;
	public static final int DW_FORM_ref_udata = 0x15;
	public static final int DW_FORM_indirect = 0x16;
	
	public static final int DW_AT_sibling = 0x01;
	public static final int DW_AT_location = 0x02;
	public static final int DW_AT_name = 0x03;
	public static final int DW_AT_ordering = 0x09;
	public static final int DW_AT_byte_size = 0x0b;
	public static final int DW_AT_bit_offset = 0x0c;
	public static final int DW_AT_bit_size = 0x0d;
	public static final int DW_AT_stmt_list = 0x10;
	public static final int DW_AT_low_pc = 0x11;
	public static final int DW_AT_high_pc = 0x12;
	public static final int DW_AT_language = 0x13;
	public static final int DW_AT_discr = 0x15;
	public static final int DW_AT_discr_value = 0x16;
	public static final int DW_AT_visibility = 0x17;
	public static final int DW_AT_import = 0x18;
	public static final int DW_AT_string_length = 0x19;
	public static final int DW_AT_common_reference = 0x1a;
	public static final int DW_AT_comp_dir = 0x1b;
	public static final int DW_AT_const_value = 0x1c;
	public static final int DW_AT_containing_type = 0x1d;
	public static final int DW_AT_default_value = 0x1e;
	public static final int DW_AT_inline = 0x20;
	public static final int DW_AT_is_optional = 0x21;
	public static final int DW_AT_lower_bound = 0x22;
	public static final int DW_AT_producer = 0x25;
	public static final int DW_AT_prototyped = 0x27;
	public static final int DW_AT_return_addr = 0x2a;
	public static final int DW_AT_start_scope = 0x2c;
	public static final int DW_AT_bit_stride = 0x2e;
	public static final int DW_AT_upper_bound = 0x2f;
	public static final int DW_AT_abstract_origin = 0x31;
	public static final int DW_AT_accessibility = 0x32;
	public static final int DW_AT_address_class = 0x33;
	public static final int DW_AT_artificial = 0x34;
	public static final int DW_AT_base_types = 0x35;
	public static final int DW_AT_calling_convention = 0x36;
	public static final int DW_AT_count = 0x37;
	public static final int DW_AT_data_member_location = 0x38;
	public static final int DW_AT_decl_column = 0x39;
	public static final int DW_AT_decl_file = 0x3a;
	public static final int DW_AT_decl_line = 0x3b;
	public static final int DW_AT_declaration = 0x3c;
	public static final int DW_AT_discr_list = 0x3d;
	public static final int DW_AT_encoding = 0x3e;
	public static final int DW_AT_external = 0x3f;
	public static final int DW_AT_frame_base = 0x40;
	public static final int DW_AT_friend = 0x41;
	public static final int DW_AT_identifier_case = 0x42;
	public static final int DW_AT_macro_info = 0x43;
	public static final int DW_AT_namelist_item = 0x44;
	public static final int DW_AT_priority = 0x45;
	public static final int DW_AT_segment = 0x46;
	public static final int DW_AT_specification = 0x47;
	public static final int DW_AT_static_link = 0x48;
	public static final int DW_AT_type = 0x49;
	public static final int DW_AT_use_location = 0x4a;
	public static final int DW_AT_variable_parameter = 0x4b;
	public static final int DW_AT_virtuality = 0x4c;
	public static final int DW_AT_vtable_elem_location = 0x4d;
	public static final int DW_AT_allocated = 0x4e;
	public static final int DW_AT_associated = 0x4f;
	public static final int DW_AT_data_location = 0x50;
	public static final int DW_AT_byte_stride = 0x51;
	public static final int DW_AT_entry_pc = 0x52;
	public static final int DW_AT_use_UTF8 = 0x53;
	public static final int DW_AT_extension = 0x54;
	public static final int DW_AT_ranges = 0x55;
	public static final int DW_AT_trampoline = 0x56;
	public static final int DW_AT_call_column = 0x57;
	public static final int DW_AT_call_file = 0x58;
	public static final int DW_AT_call_line = 0x59;
	public static final int DW_AT_description = 0x5a;
	public static final int DW_AT_binary_scale = 0x5b;
	public static final int DW_AT_decimal_scale = 0x5c;
	public static final int DW_AT_small = 0x5d;
	public static final int DW_AT_decimal_sign = 0x5e;
	public static final int DW_AT_digit_count = 0x5f;
	public static final int DW_AT_picture_string = 0x60;
	public static final int DW_AT_mutable = 0x61;
	public static final int DW_AT_threads_scaled = 0x62;
	public static final int DW_AT_explicit = 0x63;
	public static final int DW_AT_object_pointer = 0x64;
	public static final int DW_AT_endianity = 0x65;
	public static final int DW_AT_elemental = 0x66;
	public static final int DW_AT_pure = 0x67;
	public static final int DW_AT_recursive = 0x68;
	
	public static final int DW_AT_lo_user = 0x2000;
	public static final int DW_AT_hi_user = 0x3fff;
	
	public static final int DW_OP_addr = 0x03;
	public static final int DW_OP_deref = 0x06;
	public static final int DW_OP_const1u = 0x08;
	public static final int DW_OP_const1s = 0x09;
	public static final int DW_OP_const2u = 0x0a;
	public static final int DW_OP_const2s = 0x0b;
	public static final int DW_OP_const4u = 0x0c;
	public static final int DW_OP_const4s = 0x0d;
	public static final int DW_OP_const8u = 0x0e;
	public static final int DW_OP_const8s = 0x0f;
	public static final int DW_OP_constu = 0x10;
	public static final int DW_OP_consts = 0x11;
	public static final int DW_OP_dup = 0x12;
	public static final int DW_OP_drop = 0x13;
	public static final int DW_OP_over = 0x14;
	public static final int DW_OP_pick = 0x15;
	public static final int DW_OP_swap = 0x16;
	public static final int DW_OP_rot = 0x17;
	public static final int DW_OP_xderef = 0x18;
	public static final int DW_OP_abs = 0x19;
	public static final int DW_OP_and = 0x1a;
	public static final int DW_OP_div = 0x1b;
	public static final int DW_OP_minus = 0x1c;
	public static final int DW_OP_mod = 0x1d;
	public static final int DW_OP_mul = 0x1e;
	public static final int DW_OP_neg = 0x1f;
	public static final int DW_OP_not = 0x20;
	public static final int DW_OP_or = 0x21;
	public static final int DW_OP_plus = 0x22;
	public static final int DW_OP_plus_uconst = 0x23;
	public static final int DW_OP_shl = 0x24;
	public static final int DW_OP_shr = 0x25;
	public static final int DW_OP_shra = 0x26;
	public static final int DW_OP_xor = 0x27;
	public static final int DW_OP_skip = 0x2f;
	public static final int DW_OP_bra = 0x28;
	public static final int DW_OP_eq = 0x29;
	public static final int DW_OP_ge = 0x2a;
	public static final int DW_OP_gt = 0x2b;
	public static final int DW_OP_le = 0x2c;
	public static final int DW_OP_lt = 0x2d;
	public static final int DW_OP_ne = 0x2e;
	public static final int DW_OP_lit0 = 0x30;
	public static final int DW_OP_lit1 = 0x31;
	public static final int DW_OP_lit2 = 0x32;
	public static final int DW_OP_lit3 = 0x33;
	public static final int DW_OP_lit4 = 0x34;
	public static final int DW_OP_lit5 = 0x35;
	public static final int DW_OP_lit6 = 0x36;
	public static final int DW_OP_lit7 = 0x37;
	public static final int DW_OP_lit8 = 0x38;
	public static final int DW_OP_lit9 = 0x39;
	public static final int DW_OP_lit10 = 0x3a;
	public static final int DW_OP_lit11 = 0x3b;
	public static final int DW_OP_lit12 = 0x3c;
	public static final int DW_OP_lit13 = 0x3d;
	public static final int DW_OP_lit14 = 0x3e;
	public static final int DW_OP_lit15 = 0x3f;
	public static final int DW_OP_lit16 = 0x40;
	public static final int DW_OP_lit17 = 0x41;
	public static final int DW_OP_lit18 = 0x42;
	public static final int DW_OP_lit19 = 0x43;
	public static final int DW_OP_lit20 = 0x44;
	public static final int DW_OP_lit21 = 0x45;
	public static final int DW_OP_lit22 = 0x46;
	public static final int DW_OP_lit23 = 0x47;
	public static final int DW_OP_lit24 = 0x48;
	public static final int DW_OP_lit25 = 0x49;
	public static final int DW_OP_lit26 = 0x4a;
	public static final int DW_OP_lit27 = 0x4b;
	public static final int DW_OP_lit28 = 0x4c;
	public static final int DW_OP_lit29 = 0x4d;
	public static final int DW_OP_lit30 = 0x4e;
	public static final int DW_OP_lit31 = 0x4f;
	public static final int DW_OP_reg0 = 0x50;
	public static final int DW_OP_reg1 = 0x51;
	public static final int DW_OP_reg2 = 0x52;
	public static final int DW_OP_reg3 = 0x53;
	public static final int DW_OP_reg4 = 0x54;
	public static final int DW_OP_reg5 = 0x55;
	public static final int DW_OP_reg6 = 0x56;
	public static final int DW_OP_reg7 = 0x57;
	public static final int DW_OP_reg8 = 0x58;
	public static final int DW_OP_reg9 = 0x59;
	public static final int DW_OP_reg10 = 0x5a;
	public static final int DW_OP_reg11 = 0x5b;
	public static final int DW_OP_reg12 = 0x5c;
	public static final int DW_OP_reg13 = 0x5d;
	public static final int DW_OP_reg14 = 0x5e;
	public static final int DW_OP_reg15 = 0x5f;
	public static final int DW_OP_reg16 = 0x60;
	public static final int DW_OP_reg17 = 0x61;
	public static final int DW_OP_reg18 = 0x62;
	public static final int DW_OP_reg19 = 0x63;
	public static final int DW_OP_reg20 = 0x64;
	public static final int DW_OP_reg21 = 0x65;
	public static final int DW_OP_reg22 = 0x66;
	public static final int DW_OP_reg23 = 0x67;
	public static final int DW_OP_reg24 = 0x68;
	public static final int DW_OP_reg25 = 0x69;
	public static final int DW_OP_reg26 = 0x6a;
	public static final int DW_OP_reg27 = 0x6b;
	public static final int DW_OP_reg28 = 0x6c;
	public static final int DW_OP_reg29 = 0x6d;
	public static final int DW_OP_reg30 = 0x6e;
	public static final int DW_OP_reg31 = 0x6f;
	public static final int DW_OP_breg0 = 0x70;
	public static final int DW_OP_breg1 = 0x71;
	public static final int DW_OP_breg2 = 0x72;
	public static final int DW_OP_breg3 = 0x73;
	public static final int DW_OP_breg4 = 0x74;
	public static final int DW_OP_breg5 = 0x75;
	public static final int DW_OP_breg6 = 0x76;
	public static final int DW_OP_breg7 = 0x77;
	public static final int DW_OP_breg8 = 0x78;
	public static final int DW_OP_breg9 = 0x79;
	public static final int DW_OP_breg10 = 0x7a;
	public static final int DW_OP_breg11 = 0x7b;
	public static final int DW_OP_breg12 = 0x7c;
	public static final int DW_OP_breg13 = 0x7d;
	public static final int DW_OP_breg14 = 0x7e;
	public static final int DW_OP_breg15 = 0x7f;
	public static final int DW_OP_breg16 = 0x80;
	public static final int DW_OP_breg17 = 0x81;
	public static final int DW_OP_breg18 = 0x82;
	public static final int DW_OP_breg19 = 0x83;
	public static final int DW_OP_breg20 = 0x84;
	public static final int DW_OP_breg21 = 0x85;
	public static final int DW_OP_breg22 = 0x86;
	public static final int DW_OP_breg23 = 0x87;
	public static final int DW_OP_breg24 = 0x88;
	public static final int DW_OP_breg25 = 0x89;
	public static final int DW_OP_breg26 = 0x8a;
	public static final int DW_OP_breg27 = 0x8b;
	public static final int DW_OP_breg28 = 0x8c;
	public static final int DW_OP_breg29 = 0x8d;
	public static final int DW_OP_breg30 = 0x8e;
	public static final int DW_OP_breg31 = 0x8f;
	public static final int DW_OP_regx = 0x90;
	public static final int DW_OP_fbreg = 0x91;
	public static final int DW_OP_bregx = 0x92;
	public static final int DW_OP_piece = 0x93;
	public static final int DW_OP_deref_size = 0x94;
	public static final int DW_OP_xderef_size = 0x95;
	public static final int DW_OP_nop = 0x96;
	public static final int DW_OP_push_object_address = 0x97;
	public static final int DW_OP_call2 = 0x98;
	public static final int DW_OP_call4 = 0x99;
	public static final int DW_OP_call_ref = 0x9a;
	public static final int DW_OP_form_tls_address = 0x9b;
	public static final int DW_OP_call_frame_cfa = 0x9c;
	public static final int DW_OP_bit_piece = 0x9d;
	
	public static final int DW_OP_lo_user = 0xe0;
	public static final int DW_OP_hi_user = 0xff;
	
	public static final int DW_ATE_address = 0x01;
	public static final int DW_ATE_boolean = 0x02;
	public static final int DW_ATE_complex_float = 0x03;
	public static final int DW_ATE_float = 0x04;
	public static final int DW_ATE_signed = 0x05;
	public static final int DW_ATE_signed_char = 0x06;
	public static final int DW_ATE_unsigned = 0x07;
	public static final int DW_ATE_unsigned_char = 0x08;
	public static final int DW_ATE_imaginary_float = 0x09;
	public static final int DW_ATE_packed_decimal = 0x0a;
	public static final int DW_ATE_numeric_string = 0x0b;
	public static final int DW_ATE_edited = 0x0c;
	public static final int DW_ATE_signed_fixed = 0x0d;
	public static final int DW_ATE_unsigned_fixed = 0x0e;
	public static final int DW_ATE_decimal_float = 0x0f;
	
	public static final int DW_ATE_lo_user = 0x80;
	public static final int DW_ATE_hi_user = 0xff;
	
	public static final int DW_DS_unsigned = 0x01;
	public static final int DW_DS_leading_overpunch = 0x02;
	public static final int DW_DS_trailing_overpunch = 0x03;
	public static final int DW_DS_leading_separate = 0x04;
	public static final int DW_DS_trailing_separate = 0x05;
	
	public static final int DW_END_default = 0x00;
	public static final int DW_END_big = 0x01;
	public static final int DW_END_little = 0x02;
	
	public static final int DW_END_lo_user = 0x40;
	public static final int DW_END_hi_user = 0xff;
	
	public static final int DW_ACCESS_public = 0x01;
	public static final int DW_ACCESS_protected = 0x02;
	public static final int DW_ACCESS_private = 0x03;
	
	public static final int DW_VIS_local = 0x01;
	public static final int DW_VIS_exported = 0x02;
	public static final int DW_VIS_qualified = 0x03;
	
	public static final int DW_VIRTUALITY_none = 0x00;
	public static final int DW_VIRTUALITY_virtual = 0x01;
	public static final int DW_VIRTUALITY_pure_virtual = 0x02;
	
	public static final int DW_LANG_C89 = 0x0001;
	public static final int DW_LANG_C = 0x0002;
	public static final int DW_LANG_Ada83 = 0x0003;
	public static final int DW_LANG_C_plus_plus = 0x0004;
	public static final int DW_LANG_Cobol74 = 0x0005;
	public static final int DW_LANG_Cobol85 = 0x0006;
	public static final int DW_LANG_Fortran77 = 0x0007;
	public static final int DW_LANG_Fortran90 = 0x0008;
	public static final int DW_LANG_Pascal83 = 0x0009;
	public static final int DW_LANG_Modula2 = 0x000a;
	public static final int DW_LANG_Java = 0x000b;
	public static final int DW_LANG_C99 = 0x000c;
	public static final int DW_LANG_Ada95 = 0x000d;
	public static final int DW_LANG_Fortran95 = 0x000e;
	public static final int DW_LANG_PLI = 0x000f;
	public static final int DW_LANG_ObjC = 0x0010;
	public static final int DW_LANG_ObjC_plus_plus = 0x0011;
	public static final int DW_LANG_UPC = 0x0012;
	public static final int DW_LANG_D = 0x0013;
	
	public static final int DW_LANG_Mips_Assembler = 0x8001;
	
	public static final int DW_LANG_lo_user = 0x8000;
	public static final int DW_LANG_hi_user = 0xffff;
	
	public static final int DW_ID_case_sensitive = 0x00;
	public static final int DW_ID_up_case = 0x01;
	public static final int DW_ID_down_case = 0x02;
	public static final int DW_ID_case_insensitive = 0x03;
	
	public static final int DW_CC_normal = 0x01;
	public static final int DW_CC_program = 0x02;
	public static final int DW_CC_nocall = 0x03;
	
	public static final int DW_CC_lo_user = 0x40;
	public static final int DW_CC_hi_user = 0xff;
	
	public static final int DW_INL_not_inlined = 0x00;
	public static final int DW_INL_inlined = 0x01;
	public static final int DW_INL_declared_not_inlined = 0x02;
	public static final int DW_INL_declared_inlined = 0x03;
	
	public static final int DW_ORD_row_major = 0x00;
	public static final int DW_ORD_col_major = 0x01;
	
	public static final int DW_DSC_label = 0x00;
	public static final int DW_DSC_range = 0x01;
	
	public static final int DW_LNS_extended_op = 0x00;
	public static final int DW_LNS_copy = 0x01;
	public static final int DW_LNS_advance_pc = 0x02;
	public static final int DW_LNS_advance_line = 0x03;
	public static final int DW_LNS_set_file = 0x04;
	public static final int DW_LNS_set_column = 0x05;
	public static final int DW_LNS_negate_stmt = 0x06;
	public static final int DW_LNS_set_basic_block = 0x07;
	public static final int DW_LNS_const_add_pc = 0x08;
	public static final int DW_LNS_fixed_advance_pc = 0x09;
	public static final int DW_LNS_set_prologue_end = 0x0a;
	public static final int DW_LNS_set_epilogue_begin = 0x0b;
	public static final int DW_LNS_set_isa = 0x0c;
	
	public static final int DW_LNE_end_sequence = 0x01;
	public static final int DW_LNE_set_address = 0x02;
	public static final int DW_LNE_define_file = 0x03;
	public static final int DW_LNE_lo_user = 0x80;
	public static final int DW_LNE_hi_user = 0xff;
	
	public static final int DW_MACINFO_define = 0x01;
	public static final int DW_MACINFO_undef = 0x02;
	public static final int DW_MACINFO_start_file = 0x03;
	public static final int DW_MACINFO_end_file = 0x04;
	public static final int DW_MACINFO_vendor_ext = 0xff;
	
	public static final int DW_CFA_advance_loc = 0x01;
	public static final int DW_CFA_offset = 0x02;
	public static final int DW_CFA_restore = 0x03;
	public static final int DW_CFA_nop = 0x00;
	public static final int DW_CFA_set_loc = 0x01;
	public static final int DW_CFA_advance_loc1 = 0x02;
	public static final int DW_CFA_advance_loc2 = 0x03;
	public static final int DW_CFA_advance_loc4 = 0x04;
	public static final int DW_CFA_offset_extended = 0x05;
	public static final int DW_CFA_restore_extended = 0x06;
	public static final int DW_CFA_undefined = 0x07;
	public static final int DW_CFA_same_value = 0x08;
	public static final int DW_CFA_register = 0x09;
	public static final int DW_CFA_remember_state = 0x0a;
	public static final int DW_CFA_restore_state = 0x0b;
	public static final int DW_CFA_def_cfa = 0x0c;
	public static final int DW_CFA_def_cfa_register = 0x0d;
	public static final int DW_CFA_def_cfa_offset = 0x0e;
	public static final int DW_CFA_def_cfa_expression = 0x0f;
	public static final int DW_CFA_expression = 0x10;
	public static final int DW_CFA_offset_extended_sf = 0x11;
	public static final int DW_CFA_def_cfa_sf = 0x12;
	public static final int DW_CFA_def_cfa_offset_sf = 0x13;
	public static final int DW_CFA_val_offset = 0x14;
	public static final int DW_CFA_val_offset_sf = 0x15;
	public static final int DW_CFA_val_expression = 0x16;
	public static final int DW_CFA_lo_user = 0x1c;
	public static final int DW_CFA_hi_user = 0x3f;
	
	//standard opcode length for file line program opcodes
	public static final int[] STD_FILELINE_OPCODE_LEN = {0, //opcode 0: extended opcode escape - ignore in this list
			0, //opcode 1: DW_LNS_copy, no operands
			1, //opcode 2: DW_LNS_advance_pc, 1 unsigned LEB128 operand (increase address and op_index register)
			1, //opcode 3: DW_LNS_advance_line, 1 signed LEB128 operand (in/decrease line register)
			1, //opcode 4: DW_LNS_set_file, 1 unsigned LEB128 operand (set file register)
			1, //opcode 5: DW_LNS_set_column, 1 unsigned LEB128 operand (set column register)
			0, //opcode 6: DW_LNS_negate_stmt, no operands
			0, //opcode 7: DW_LNS_set_basic_block, no operands
			0, //opcode 8: DW_LNS_const_add_pc, no operands
			1, //opcode 9: DW_LNS_fixed_advance_pc, unsigned half operand (increase address register, set op_index to 0)
			0, //opcode 10: DW_LNS_set_prologue_end, no operands
			0, //opcode 11: DW_LNS_set_epilogue_begin
			1  //opcode 12: DW_LNS_set_isa, 1 signed LEB128 (set isa register)
	};
	
	//our variables
	private final Context ctx;
	private final String fname;
	private BinWriter bin;
	private StringList shstrtab, dbgstr;
	private final TextBuffer txtBuf;
	private final Section[] sections;
	private final Section info;
	private final Section abbrev;
	private final Section str;
	private final Section aranges;
	private final Section line;
	private int abbrevCnt, cuHeaderStartInfo, cuLineStart;
	
	//abbreviation numbers
	private final int unitAbbrev;
	private final int baseTypeAbbrev;
	private final int mthdAbbrev/*, vrblAbbrev*/;
	
	//base types
  /*private int typeBoolean, typeByte, typeShort, typeInt, typeLong;
  private int typeFloat, typeDouble, typeChar, typePointer, typeDPointer;*/
	
	//some internal constants
	private final static int BUF_INC = 1000;
	private final static int LINE_BASE = 0;
	private final static int LINE_RANGE = 9;
	
	private final static byte[] ELF_Ehdr = {0x7F, 'E', 'L', 'F', 1, 1, 1,      //magic
			0, 0, 0, 0, 0, 0, 0, 0, 0,         //elf-internals
			2, 0,                              //type
			3, 0,                              //machine
			1, 0, 0, 0,                        //version
			0, 0, 0, 0,                        //entry
			0, 0, 0, 0,                        //pointer to Phdr
			52, 0, 0, 0,                       //pointer to Shdr (unused)
			0, 0, 0, 0,                        //flags
			52, 0,                             //sizeof Ehdr
			0, 0,                              //sizeof Phdr
			0, 0, 40, 0,                       //phnum, shentsize
			(byte) SECTION_COUNT, 0,            //shnum
			(byte) SEC_SHSTRTAB, 0              //shstrtab section index
	};
	
	private class Section
	{ //dumb container class
		public String name;
		public int type, link, align, entrySize;
		public byte[] byteBuf;
		
		public int startInFile; //has to be set before writeHeader is called
		public int usedBytes;
		
		public Section(String iname, int itype, int ilink, int ialign, int iesize)
		{
			name = iname;
			type = itype;
			link = ilink;
			align = ialign;
			entrySize = iesize;
			addShStrTabEntry(name);
		}
		
		public void writeHeader()
		{
			byte[] hdr = new byte[SECTION_HEADER_SIZE];
			if (type != SHT_NULL)
			{ //fill only if not null segment
				replaceInt(hdr, 0 * 4, getShStrTabOffset(name));
				replaceInt(hdr, 1 * 4, type);
				replaceInt(hdr, 4 * 4, startInFile);
				replaceInt(hdr, 5 * 4, usedBytes);
				replaceInt(hdr, 6 * 4, link);
				replaceInt(hdr, 8 * 4, align);
				replaceInt(hdr, 9 * 4, entrySize);
			}
			bin.write(hdr, 0, hdr.length);
		}
		
		public void appendByte(int value)
		{ //append lowest 8 bits of value
			if (byteBuf == null || usedBytes + 1 >= byteBuf.length)
			{
				byte[] newBuf = new byte[byteBuf == null ? BUF_INC : byteBuf.length + BUF_INC];
				for (int i = 0; i < usedBytes; i++)
					newBuf[i] = byteBuf[i];
				byteBuf = newBuf;
			}
			byteBuf[usedBytes++] = (byte) value;
		}
		
		public void appendTwoBytes(int v1, int v2)
		{ //append lowest 8 bits of each value
			appendByte(v1);
			appendByte(v2);
		}
		
		public void appendShort(int value)
		{ //append lowest 16 bits of value
			appendByte(value);
			appendByte(value >>> 8);
		}
		
		public void appendInt(int value)
		{ //append all 32 bits of value
			appendByte(value);
			appendByte(value >>> 8);
			appendByte(value >>> 16);
			appendByte(value >>> 24);
		}
		
		public void appendUnsignedLEB128(int value)
		{ //append int (unsigned) of arbitraty length in LEB128 encoding
			while (true)
			{
				if ((value & ~0x7F) == 0)
				{
					appendByte(value);
					return;
				}
				appendByte((value & 0x7F) | 0x80);
				value >>>= 7;
			}
		}
		
		public void appendSignedLEB128(int value)
		{ //append int (signed) of arbitraty length in LEB128 encoding
			while (true)
			{
				int b = value & 0x7F;
				value >>= 7;
				if ((value == -1 && (b & 0x40) != 0) || (value == 0 && (b & 0x40) == 0))
				{
					appendByte(b);
					return;
				}
				appendByte(b | 0x80);
			}
		}
		
		public void appendNullString(String value)
		{
			int len = value.length();
			for (int i = 0; i < len; i++)
				appendByte(value.charAt(i));
			appendByte(0); //null-byte
		}
	}
	
	public Dwarf(String filename, Context ictx)
	{
		fname = filename;
		ctx = ictx;
		txtBuf = new TextBuffer();
		
		//allocate and init sections
		sections = new Section[SECTION_COUNT];
		sections[SEC_NULL] = new Section("", SHT_NULL, 0, 1, 0);
		sections[SEC_SHSTRTAB] = new Section(".shstrtab", SHT_STRTAB, 0, 1, 0);
		sections[SEC_DEB_ARANGES] = aranges = new Section(".debug_aranges", SHT_PROGBITS, 0, 1, 0);
		sections[SEC_DEB_INFO] = info = new Section(".debug_info", SHT_PROGBITS, 0, 1, 0);
		sections[SEC_DEB_ABBREV] = abbrev = new Section(".debug_abbrev", SHT_PROGBITS, 0, 1, 0);
		sections[SEC_DEB_LINE] = line = new Section(".debug_line", SHT_PROGBITS, 0, 1, 0);
		sections[SEC_DEB_STR] = str = new Section(".debug_str", SHT_PROGBITS, 0, 1, 0);
		sections[SEC_DEB_LOC] = new Section(".debug_loc", SHT_PROGBITS, 0, 1, 0);
		
		//create unit abbreviation
		unitAbbrev = ++abbrevCnt;
		abbrev.appendUnsignedLEB128(unitAbbrev);
		abbrev.appendByte(DW_TAG_compile_unit);
		abbrev.appendByte(DW_CHILDREN_yes);
		abbrev.appendTwoBytes(DW_AT_name, DW_FORM_strp);
		abbrev.appendTwoBytes(DW_AT_comp_dir, DW_FORM_strp);
		abbrev.appendTwoBytes(DW_AT_producer, DW_FORM_strp);
		abbrev.appendTwoBytes(DW_AT_language, DW_FORM_data1);
		abbrev.appendTwoBytes(DW_AT_low_pc, DW_FORM_addr);
		abbrev.appendTwoBytes(DW_AT_high_pc, DW_FORM_addr);
		abbrev.appendTwoBytes(DW_AT_stmt_list, DW_FORM_data4);
		abbrev.appendTwoBytes(0, 0);
		//create type abbreviation
		baseTypeAbbrev = ++abbrevCnt;
		abbrev.appendUnsignedLEB128(baseTypeAbbrev);
		abbrev.appendByte(DW_TAG_base_type);
		abbrev.appendByte(DW_CHILDREN_yes);
		abbrev.appendTwoBytes(DW_AT_byte_size, DW_FORM_data1);
		abbrev.appendTwoBytes(DW_AT_encoding, DW_FORM_data1);
		abbrev.appendTwoBytes(DW_AT_name, DW_FORM_strp);
		abbrev.appendTwoBytes(0, 0);
		//create method abbreviation
		mthdAbbrev = ++abbrevCnt;
		abbrev.appendUnsignedLEB128(mthdAbbrev);
		abbrev.appendByte(DW_TAG_subprogram);
		abbrev.appendByte(DW_CHILDREN_yes);
		abbrev.appendTwoBytes(DW_AT_name, DW_FORM_strp);
		abbrev.appendTwoBytes(DW_AT_external, DW_FORM_flag);
		abbrev.appendTwoBytes(DW_AT_low_pc, DW_FORM_addr);
		abbrev.appendTwoBytes(DW_AT_high_pc, DW_FORM_addr);
		abbrev.appendTwoBytes(0, 0);
		
		//this is not really working - therefore disabled
    /*
    //create variable abbreviation (used for static non-final vars)
    vrblAbbrev=++abbrevCnt;
    abbrev.appendUnsignedLEB128(vrblAbbrev);
    abbrev.appendByte(DW_TAG_variable);
    abbrev.appendByte(DW_CHILDREN_yes);
    abbrev.appendTwoBytes(DW_AT_name, DW_FORM_strp);
    abbrev.appendTwoBytes(DW_AT_location, DW_FORM_data4);
    abbrev.appendTwoBytes(DW_AT_type, DW_FORM_ref_addr);
    abbrev.appendTwoBytes(0, 0);
    
    //create base types in dummy compilation unit
    cuHeaderStartInfo=info.usedBytes; //remember start of compilation unit header
    info.appendInt(0); //dummy-length, has to be replaced at end of unit
    info.appendShort(3); //version
    info.appendInt(0); //abbrev_offset
    info.appendByte(4); //address size
    //enter values/offsets in info (see unit abbrev definition), put strings into str 
    info.appendUnsignedLEB128(unitAbbrev);
    info.appendInt(getDbgStringOffset("--SJC--TypeList--"));
    info.appendInt(getDbgStringOffset(""));
    info.appendInt(getDbgStringOffset("SJC"));
    info.appendByte(DW_LANG_Java);
    info.appendInt(0);
    info.appendInt(0);
    info.appendInt(0); //no line info
    typeBoolean=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(1); //size
    info.appendByte(DW_ATE_boolean); //encoding
    info.appendInt(getDbgStringOffset("boolean")); //name
    info.appendByte(0); //base type done
    typeByte=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(1); //size
    info.appendByte(DW_ATE_signed); //encoding
    info.appendInt(getDbgStringOffset("byte")); //name
    info.appendByte(0); //base type done
    typeShort=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(2); //size
    info.appendByte(DW_ATE_signed); //encoding
    info.appendInt(getDbgStringOffset("short")); //name
    info.appendByte(0); //base type done
    typeInt=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(4); //size
    info.appendByte(DW_ATE_signed); //encoding
    info.appendInt(getDbgStringOffset("int")); //name
    info.appendByte(0); //base type done
    typeLong=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(8); //size
    info.appendByte(DW_ATE_signed); //encoding
    info.appendInt(getDbgStringOffset("long")); //name
    info.appendByte(0); //base type done
    typeChar=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(2); //size
    info.appendByte(DW_ATE_unsigned_char); //encoding
    info.appendInt(getDbgStringOffset("char")); //name
    info.appendByte(0); //base type done
    typeFloat=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(4); //size
    info.appendByte(DW_ATE_float); //encoding
    info.appendInt(getDbgStringOffset("float")); //name
    info.appendByte(0); //base type done
    typeDouble=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(8); //size
    info.appendByte(DW_ATE_float); //encoding
    info.appendInt(getDbgStringOffset("double")); //name
    info.appendByte(0); //base type done
    typePointer=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(4); //size
    info.appendByte(DW_ATE_address); //encoding
    info.appendInt(getDbgStringOffset("PTR")); //name
    info.appendByte(0); //base type done
    typeDPointer=info.usedBytes;
    info.appendUnsignedLEB128(baseTypeAbbrev);
    info.appendByte(8); //size
    info.appendByte(DW_ATE_address); //encoding
    info.appendInt(getDbgStringOffset("DPTR")); //name
    info.appendByte(0); //base type done
    info.appendByte(0); //end of unit
    replaceInt(info.byteBuf, cuHeaderStartInfo, info.usedBytes-(cuHeaderStartInfo+4)); //enter size without length field
    */
	}
	
	public void startImageInfo(boolean isDecompressor)
	{
		if (!ctx.globalSourceLineHints)
			ctx.out.println("dwarf: slhi not globally enabled, use \"-Q\" to get line number infos");
	}
	
	public void finalizeImageInfo()
	{
		//finalize abbreviation section
		abbrev.appendByte(0);
		//get something to write to
		bin = ctx.osio.getNewBinWriter();
		if (!bin.open(fname))
		{
			ctx.out.print("Error opening output-file ");
			ctx.out.print(fname);
			ctx.out.println(" for dwarf-output, skipping file");
			return;
		}
		//write ELF header
		bin.write(ELF_Ehdr, 0, ELF_Ehdr.length);
		//build shstrtab section
		finalizeShStrTab();
		//align sections and set their file offset
		int dataLen = 0;
		for (int i = 0; i < SECTION_COUNT; i++)
		{
			dataLen = (dataLen + sections[i].align - 1) & ~(sections[i].align - 1);
			sections[i].startInFile = dataLen + DATASTART_IN_FILE;
			dataLen += sections[i].usedBytes;
		}
		//write section headers
		for (int i = 0; i < SECTION_COUNT; i++)
			sections[i].writeHeader();
		//write sections
		for (int i = 0; i < SECTION_COUNT; i++)
		{
			if (sections[i].byteBuf != null)
				bin.write(sections[i].byteBuf, 0, sections[i].usedBytes);
		}
		//ok, file is complete
		bin.close();
	}
	
	public void globalMemoryInfo(int baseAddress, int memBlockLen)
	{
	}
	
	public void globalMethodInfo(int mthdCodeSize, int mthdCount)
	{
	}
	
	public void globalStringInfo(int stringCount, int stringChars, int stringMemBytes)
	{
	}
	
	public void globalRAMInfo(Object ramInitLoc, int ramSize, int constMemorySize)
	{
	}
	
	public void globalSymbolInfo(int symGenSize)
	{
	}
	
	public void startUnit(String unitType, Unit unit)
	{
		//get range of code
		int lowestPc = 0x7FFFFFFF, highestPc = 0;
		Mthd mthd = unit.mthds;
		while (mthd != null)
		{
			if (mthd.codeSize > 0)
			{
				int start = ctx.mem.getAddrAsInt(mthd.outputLocation, ctx.codeStart);
				int end = start + mthd.codeSize;
				if (start < lowestPc)
					lowestPc = start;
				if (end > highestPc)
					highestPc = end;
			}
			mthd = mthd.nextMthd;
		}
		if (highestPc == 0)
			lowestPc = 0;
		//compilation unit in info-section, see dwarf 7.5.1.1
		cuHeaderStartInfo = info.usedBytes; //remember start of compilation unit header
		info.appendInt(0); //dummy-length, has to be replaced at end of unit
		info.appendShort(3); //version
		info.appendInt(0); //abbrev_offset
		info.appendByte(4); //address size
		//enter values/offsets in info (see unit abbrev definition), put strings into str
		info.appendUnsignedLEB128(unitAbbrev);
		String fullpath = ctx.getNameOfFile(unit.fileID);
		String file = "", dir = "";
		if (fullpath != null)
		{
			int slash = fullpath.lastIndexOf('/');
			int slWin = fullpath.lastIndexOf('\\');
			if (slWin > slash)
				slash = slWin;
			if (slash >= 0)
			{
				file = fullpath.substring(slash + 1);
				dir = fullpath.substring(0, slash);
			}
			else
				file = fullpath;
		}
		if (file == null || file.equals(""))
			file = "[intern]"; //should never happen
		txtBuf.reset();
		unit.getQIDTo().printFullQID(txtBuf);
		info.appendInt(getDbgStringOffset(new String(txtBuf.data, 0, txtBuf.used)));
		info.appendInt(getDbgStringOffset(dir));
		info.appendInt(getDbgStringOffset("SJC"));
		info.appendByte(DW_LANG_Java);
		info.appendInt(lowestPc);
		info.appendInt(highestPc);
		if (highestPc - lowestPc > 0)
		{ //write only if there is binary code which needs information
			//compilation unit in aranges-section, see dwarf 6.1.2 and 7.20
			int cuHStartARanges = aranges.usedBytes;
			aranges.appendInt(0); //dummy-length, has to be replaced afterwards
			aranges.appendShort(2); //version
			aranges.appendInt(cuHeaderStartInfo); //referenced compilation unit in info section
			aranges.appendByte(4); //address size
			aranges.appendByte(0); //segment size == flat memory model, no segment in table => table consists of pairs
			aranges.appendInt(0); //padding to have list starting at a multiple of (2*address_size)
			//there is one entry all the methods, write start/length
			aranges.appendInt(lowestPc);
			aranges.appendInt(highestPc - lowestPc);
			//finalize aranges section table for this compilation unit by adding a 0-0-pair
			aranges.appendInt(0);
			aranges.appendInt(0);
			//fixup compilation unit size in aranges-section
			replaceInt(aranges.byteBuf, cuHStartARanges, aranges.usedBytes - (cuHStartARanges + 4)); //replace length of this cuHeader
			//compilation unit in line-section, see dwarf 6.2.4, test with objdump --dwarf=decodedline FILE or dwarfdump -l [-vv] FILE
			cuLineStart = line.usedBytes;
			info.appendInt(cuLineStart);
			line.appendInt(0); //dummy-length, has to be replaced afterwards
			line.appendShort(3); //version
			int lineHeaderStart = line.usedBytes;
			line.appendInt(0); //dummy-header-length
			int lineHeaderFollowing = line.usedBytes;
			line.appendByte(1); //minimum instruction length
			//do not place maximum operations per instructions)
			line.appendByte(1); //default is stmt
			line.appendByte(LINE_BASE); //line base (add 1 to line register for a standard opcode)
			line.appendByte(LINE_RANGE); //line range (add less than 6 lines at once for a standard opcode)
			line.appendByte(STD_FILELINE_OPCODE_LEN.length); //opcode base == first special opcode
			for (int i = 1; i < STD_FILELINE_OPCODE_LEN.length; i++)
				line.appendUnsignedLEB128(STD_FILELINE_OPCODE_LEN[i]); //standard opcode lengths
			line.appendByte(0); //include directories
			line.appendNullString(file);
			line.appendUnsignedLEB128(0); //file name: directory index
			line.appendUnsignedLEB128(0); //file name: last modification (not available)
			line.appendUnsignedLEB128(0); //file name: file length (not available)
			line.appendByte(0); //finish file names list
			replaceInt(line.byteBuf, lineHeaderStart, line.usedBytes - lineHeaderFollowing);
		}
		else
		{ //remember that there is nothing to end
			cuLineStart = -1;
			info.appendInt(0); //no line info
		}
		//enter base types
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(1); //size
		info.appendByte(DW_ATE_boolean); //encoding
		info.appendInt(getDbgStringOffset("boolean")); //name
		info.appendByte(0); //base type done
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(1); //size
		info.appendByte(DW_ATE_signed); //encoding
		info.appendInt(getDbgStringOffset("byte")); //name
		info.appendByte(0); //base type done
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(2); //size
		info.appendByte(DW_ATE_signed); //encoding
		info.appendInt(getDbgStringOffset("short")); //name
		info.appendByte(0); //base type done
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(4); //size
		info.appendByte(DW_ATE_signed); //encoding
		info.appendInt(getDbgStringOffset("int")); //name
		info.appendByte(0); //base type done
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(8); //size
		info.appendByte(DW_ATE_signed); //encoding
		info.appendInt(getDbgStringOffset("long")); //name
		info.appendByte(0); //base type done
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(2); //size
		info.appendByte(DW_ATE_unsigned_char); //encoding
		info.appendInt(getDbgStringOffset("char")); //name
		info.appendByte(0); //base type done
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(4); //size
		info.appendByte(DW_ATE_float); //encoding
		info.appendInt(getDbgStringOffset("float")); //name
		info.appendByte(0); //base type done
		info.appendUnsignedLEB128(baseTypeAbbrev);
		info.appendByte(8); //size
		info.appendByte(DW_ATE_float); //encoding
		info.appendInt(getDbgStringOffset("double")); //name
		info.appendByte(0); //base type done
	}
	
	public void markUnitAsNotUsed()
	{
	}
	
	public void hasUnitOutputLocation(Object outputLocation)
	{
	}
	
	public void hasUnitFields(int clssRelocTableEntries, int clssScalarTableSize, int statRelocTableEntries, int statScalarTableSize, int instRelocTableEntries, int instScalarTableSize, int instIndirScalarTableSize)
	{
	}
	
	public void startVariableList()
	{
	}
	
	public void hasVariable(Vrbl var)
	{
		//this is not really working - therefore disabled
    /*
    switch (var.location) {
      case AccVar.L_CONST: //resolved constant
        break;
      case AccVar.L_CLSSSCL: //scalar inside class
      case AccVar.L_CLSSREL: //reloc inside class
        info.appendUnsignedLEB128(vrblAbbrev);
        info.appendInt(getDbgStringOffset(var.name));
        info.appendInt(ctx.dynaMem ? ctx.mem.getAddrAsInt(var.owner.outputLocation, var.relOff) : ctx.mem.getAddrAsInt(!ctx.embedded ? var.owner.outputLocation : ctx.ramLoc, var.relOff));
        info.appendInt(getDbgTypeOffset(var.type));
        info.appendByte(0); //end of variable data
        break;
      case AccVar.L_INSTSCL: //scalar inside instance
        break;
      case AccVar.L_INSTIDS: //indirect accessed scalar inside instance
        break;
      case AccVar.L_INSTREL: //reloc inside instance
        break;
      case AccVar.L_STRUCT: //struct variable
        break;
      case AccVar.L_STRUCTREF: //struct reference with reference to other struct
        break;
      case AccVar.L_INLARR:
        break;
      default:
        break;
    }
    */
	}
	
	public void endVariableList()
	{
	}
	
	public void startMethodList()
	{
	}
	
	public void hasMethod(Mthd mthd, boolean indir)
	{
		if (mthd.codeSize > 0)
		{ //only output info for really generated methods
			//enter values/offsets in info (see mthd abbrev definition)
			info.appendUnsignedLEB128(mthdAbbrev);
			info.appendInt(getDbgStringOffset(mthd.name));
			info.appendByte((mthd.modifier & Modifier.M_PRIV) == 0 ? 1 : 0);
			int codeStart = ctx.mem.getAddrAsInt(mthd.outputLocation, ctx.codeStart);
			int codeEnd = codeStart + mthd.codeSize;
			info.appendInt(codeStart);
			info.appendInt(codeEnd);
			info.appendByte(0); //end of method data
			
			//enter values/offsets in line (see dwarf line program spec)
			if (mthd.lineInCodeOffset != null)
			{
				line.appendByte(DW_LNS_set_file);
				line.appendUnsignedLEB128(1);
				line.appendByte(0); //escape sequence for DW_LNE_*
				line.appendUnsignedLEB128(1 + 4);
				line.appendByte(DW_LNE_set_address);
				line.appendInt(codeStart);
				line.appendByte(DW_LNS_set_prologue_end);
				int lastOffset = 0;
				int lastLine = 0;
				for (int i = 0; i < mthd.lineInCodeOffset.length; i += 2)
				{
					int curOffset = mthd.lineInCodeOffset[i];
					int curLine = mthd.lineInCodeOffset[i + 1];
					int incOffset = curOffset - lastOffset;
					int incLine = curLine - lastLine;
					int special = (incLine - LINE_BASE) + (LINE_RANGE * incOffset) + STD_FILELINE_OPCODE_LEN.length;
					if (incLine < LINE_RANGE && special <= 255)
						line.appendByte(special); //encode as special
					else
					{ //encoding explicitly
						line.appendByte(DW_LNS_advance_line);
						line.appendSignedLEB128(incLine);
						line.appendByte(DW_LNS_advance_pc);
						line.appendUnsignedLEB128(incOffset);
						line.appendByte(DW_LNS_copy);
					}
					lastOffset = curOffset;
					lastLine = curLine;
				}
				line.appendByte(0); //escape sequence for DW_LNE_*
				line.appendUnsignedLEB128(1 + 0);
				line.appendByte(DW_LNE_end_sequence);
			}
		}
	}
	
	public void endMethodList()
	{
	}
	
	public void startStatObjList()
	{
	}
	
	public void hasStatObj(int rela, Object loc, String value, boolean inFlash)
	{
	}
	
	public void endStatObjList()
	{
	}
	
	public void startImportedUnitList()
	{
	}
	
	public void hasImportedUnit(UnitList unit)
	{
	}
	
	public void endImportedUnitList()
	{
	}
	
	public void startInterfaceMapList()
	{
	}
	
	public void hasInterfaceMap(IndirUnitMapList intf)
	{
	}
	
	public void endInterfaceMapList()
	{
	}
	
	public void endUnit()
	{
		//end compilation unit
		info.appendByte(0);
		//fixup compilation unit size in info-section
		replaceInt(info.byteBuf, cuHeaderStartInfo, info.usedBytes - (cuHeaderStartInfo + 4)); //enter size without length field
		//fixup compilation unit line information if existing
		if (cuLineStart != -1)
		{
			replaceInt(line.byteBuf, cuLineStart, line.usedBytes - (cuLineStart + 4)); //enter size without length field
		}
	}
	
	private void replaceInt(byte[] buf, int offset, int value)
	{
		buf[offset++] = (byte) value;
		buf[offset++] = (byte) (value >>> 8);
		buf[offset++] = (byte) (value >>> 16);
		buf[offset] = (byte) (value >>> 24);
	}
	
	private void addShStrTabEntry(String name)
	{
		int pos = 0;
		if (shstrtab == null)
			shstrtab = new StringList(name);
		else
		{
			StringList last = shstrtab;
			while (last.next != null)
			{
				last = last.next;
				pos++;
			}
			last.next = new StringList(name);
			last.next.tablePos = pos + 1;
		}
	}
	
	private int getShStrTabOffset(String name)
	{
		int offset = 0;
		StringList search = shstrtab;
		while (search != null)
		{
			if (search.str.equals(name))
				return offset;
			offset += search.str.length() + 1;
			search = search.next;
		}
		return -1;
	}
	
	private int getDbgStringOffset(String value)
	{
		if (value == null)
			value = "";
		StringList search = dbgstr;
		while (search != null)
		{ //try to recycle string
			if (search.str.equals(value))
				return search.tablePos;
			search = search.next;
		}
		//string not found, add it
		search = new StringList(value);
		search.tablePos = str.usedBytes;
		str.appendNullString(value);
		search.next = dbgstr;
		dbgstr = search;
		return search.tablePos;
	}
	
	//this is part of variable support, which is not really working - therefore disabled
  /*
  private int getDbgTypeOffset(TypeRef type) {
    if (type.arrDim==0) switch (type.baseType) {
      case TypeRef.T_BOOL: return typeBoolean;
      case TypeRef.T_BYTE: return typeByte;
      case TypeRef.T_SHRT: return typeShort;
      case TypeRef.T_INT: return typeInt;
      case TypeRef.T_LONG: return typeLong;
      case TypeRef.T_FLT: return typeFloat;
      case TypeRef.T_DBL: return typeDouble;
      case TypeRef.T_CHAR: return typeChar;
    }
    if (type.isIntfType()) return typeDPointer;
    return typePointer;
  }
  */
	
	private void finalizeShStrTab()
	{
		Section s = sections[SEC_SHSTRTAB];
		StringList search = shstrtab;
		while (search != null)
		{
			s.appendNullString(search.str);
			search = search.next;
		}
	}
}
