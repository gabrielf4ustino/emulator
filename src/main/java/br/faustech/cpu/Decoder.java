package br.faustech.cpu;

public class Decoder {

  // Method to decode instructions
  public static String decodeInstruction(int instruction) {

    int opcode = instruction & 0x7F; // Mask for 7 bits

    return switch (opcode) {
      case 0x33 -> decodeRType(instruction);              // R-Type
      case 0x67 -> decodeITypeJumpAndLinkRegister(instruction); // I-Type-jalr
      case 0x03 -> decodeITypeLoad(instruction);          // I-Type-load
      case 0x13 -> decodeITypeImmediate(instruction);     // I-Type-immediate
      case 0x73 -> decodeITypeControlStatusRegister(instruction); // I-Type-csr
      case 0x23 -> decodeSType(instruction);              // S-Type
      case 0x63 -> decodeBType(instruction);              // B-Type
      case 0x37, 0x17 -> decodeUType(instruction);        // U-Type
      case 0x6F -> decodeJType(instruction);              // J-Type
      default -> "Unknown Type";
    };
  }

  // Decode the R Type instruction
  private static String decodeRType(int instruction) {

    int funct3 = (instruction >> 12) & 0x7;
    int funct7 = (instruction >> 25) & 0x7F;
    int rd = (instruction >> 7) & 0x1F;
    int rs1 = (instruction >> 15) & 0x1F;
    int rs2 = (instruction >> 20) & 0x1F;

    String operation = switch (funct3) {
      case 0b000 -> (funct7 == 0) ? "add" : "sub";
      case 0b001 -> "sll";
      case 0b010 -> "slt";
      case 0b011 -> "sltu";
      case 0b100 -> "xor";
      case 0b101 -> (funct7 == 0) ? "srl" : "sra";
      case 0b110 -> "or";
      case 0b111 -> "and";
      default -> "unknown";
    };

    return String.format("%s rd=%d, rs1=%d, rs2=%d", operation, rd, rs1, rs2);
  }

  // Decode the I Type instruction for Jump and Link Register
  private static String decodeITypeJumpAndLinkRegister(int instruction) {

    int imm = instruction >> 20;
    int rs1 = (instruction >> 15) & 0x1F;
    int rd = (instruction >> 7) & 0x1F;

    return String.format("jalr rd=%d, rs1=%d, imm=%d", rd, rs1, imm);
  }

  // Decode the I Type instruction for Loads
  private static String decodeITypeLoad(int instruction) {

    int imm = instruction >> 20;
    int rs1 = (instruction >> 15) & 0x1F;
    int funct3 = (instruction >> 12) & 0x7;
    int rd = (instruction >> 7) & 0x1F;

    String operation = switch (funct3) {
      case 0b000 -> "lb";
      case 0b001 -> "lh";
      case 0b010 -> "lw";
      case 0b100 -> "lbu";
      case 0b101 -> "lhu";
      default -> "unknown";
    };

    return String.format("%s rd=%d, rs1=%d, imm=%d", operation, rd, rs1, imm);
  }

  // Decode the I Type instruction for Immediates
  private static String decodeITypeImmediate(int instruction) {

    int imm = instruction >> 20;
    int rs1 = (instruction >> 15) & 0x1F;
    int funct3 = (instruction >> 12) & 0x7;
    int rd = (instruction >> 7) & 0x1F;
    boolean aux = false;
    String operation = switch (funct3) {
      case 0b000 -> "addi";
      case 0b010 -> "slti";
      case 0b011 -> "sltiu";
      case 0b100 -> "xori";
      case 0b110 -> "ori";
      case 0b111 -> "andi";
      case 0b001 -> {
        imm &= 0x1F;
        aux = true;
        yield "slli";
      }
      case 0b101 -> {
        String op = (imm & 0xFE0) == 0 ? "srli" : "srai";
        imm &= 0x1F;
        aux = true;
        yield op;
      }
      default -> "unknown";
    };
    return aux ? String.format("%s rd=%d, rs1=%d, shamt=%d", operation, rd, rs1, imm)
        : String.format("%s rd=%d, rs1=%d, imm=%d", operation, rd, rs1, imm);

  }

  // Decode the I Type instruction for Control Status Register Atomic Operations
  private static String decodeITypeControlStatusRegister(int instruction) {

    int csr = instruction >> 20;
    int rs1_or_zimm = (instruction >> 15) & 0x1F;
    int funct3 = (instruction >> 12) & 0x7;
    int rd = (instruction >> 7) & 0x1F;

    String operation = switch (funct3) {
      case 0b000 -> (csr == 0) ? "ecall" : "ebreak";
      case 0b001 -> "csrrw";
      case 0b010 -> "csrrs";
      case 0b011 -> "csrrc";
      case 0b101 -> "csrrwi";
      case 0b110 -> "csrrsi";
      case 0b111 -> "csrrci";
      default -> "unknown";
    };

    return (funct3 == 0b000) ? operation
        : String.format("%s rd=%d, csr=%d, rs1=%d", operation, rd, csr, rs1_or_zimm);
  }

  // Decode the S Type instruction
  private static String decodeSType(int instruction) {

    int imm11_5 = (instruction >> 25) & 0x7F;
    int rs2 = (instruction >> 20) & 0x1F;
    int rs1 = (instruction >> 15) & 0x1F;
    int funct3 = (instruction >> 12) & 0x7;
    int imm4_0 = (instruction >> 7) & 0x1F;
    int imm = (imm11_5 << 5) | imm4_0;

    String operation = switch (funct3) {
      case 0b000 -> "sb";
      case 0b001 -> "sh";
      case 0b010 -> "sw";
      default -> "unknown";
    };

    return String.format("%s rs1=%d, rs2=%d, imm=%d", operation, rs1, rs2, imm);
  }

  // Decode the B Type instruction
  private static String decodeBType(int instruction) {

    int imm12 = (instruction >> 31) & 0x01;
    int imm10_5 = (instruction >> 25) & 0x3F;
    int rs2 = (instruction >> 20) & 0x1F;
    int rs1 = (instruction >> 15) & 0x1F;
    int funct3 = (instruction >> 12) & 0x7;
    int imm4_1 = (instruction >> 8) & 0xF;
    int imm11 = (instruction >> 7) & 0x1;
    int imm = (imm12 << 11) | (imm11 << 10) | (imm10_5 << 4) | (imm4_1);

    String operation = switch (funct3) {
      case 0b000 -> "beq";
      case 0b001 -> "bne";
      case 0b100 -> "blt";
      case 0b101 -> "bge";
      case 0b110 -> "bltu";
      case 0b111 -> "bgeu";
      default -> "unknown";
    };

    return String.format("%s rs1=%d, rs2=%d, imm=%d", operation, rs1, rs2, imm);
  }

  // Decode the U Type instruction
  private static String decodeUType(int instruction) {

    int imm31_12 = instruction >> 12;
    int rd = (instruction >> 7) & 0x1F;
    int opcode = instruction & 0x7F;

    String operation = switch (opcode) {
      case 0b0110111 -> "lui";
      case 0b0010111 -> "auipc";
      default -> "unknown";
    };

    return String.format("%s rd=%d, imm=%d", operation, rd, imm31_12);
  }

  // Decode the J Type instruction
  private static String decodeJType(int instruction) {

    int imm20 = (instruction >> 31) & 0x1;
    int imm10_1 = (instruction >> 21) & 0x3FF;
    int imm11 = (instruction >> 20) & 0x1;
    int imm19_12 = (instruction >> 12) & 0xFF;
    int rd = (instruction >> 7) & 0x1F;
    int imm = (imm20 << 19) | (imm19_12 << 11) | (imm11 << 10) | (imm10_1);

    return String.format("jal rd=%d, imm=%d", rd, imm);
  }

}

