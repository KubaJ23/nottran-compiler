package uk.ac.nott.cs.comp3012.coursework.util.types;

import uk.ac.nott.cs.comp3012.coursework.SymbolTable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public sealed interface Type {
    enum Intrinsic implements Type {
        INTEGER, LOGICAL, CHARACTER;

        @Override
        public String toString() {
            return this.name();
        }
    }

    record DerivedType(String typeName, LinkedHashMap<String, Type> fields) implements Type {
        @Override
        public String toString() {
            return "DERIVED_TYPE";
        }
    }

    record ArrayType(Type baseType, List<Dimension> dimensions) implements Type {
        @Override
        public String toString() {
            String dims = dimensions.stream().map(d -> {
                if (d instanceof Dimension.Known(int size)) return String.valueOf(size);
                if (d instanceof Dimension.Unknown) return "*";
                return "?";
            }).collect(Collectors.joining(", "));
            return baseType.toString() + " ARRAY(" + dims + ")";
        }
    }

    record PointerType(Type baseType) implements Type {
        @Override
        public String toString() {
            return baseType.toString() + " POINTER";
        }
    }

    record FunctionType(Type returnType, List<Type> paramTypes) implements Type {
        @Override
        public String toString() {
            String params = paramTypes.stream().map(Type::toString).collect(Collectors.joining(", "));
            return "FUNCTION(" + params + ") RETURNS " + returnType;
        }
    }

    record SubroutineType(List<Type> paramTypes) implements Type {
        @Override
        public String toString() {
            String params = paramTypes.stream().map(Type::toString).collect(Collectors.joining(", "));
            return "SUBROUTINE(" + params + ")";
        }
    }
}