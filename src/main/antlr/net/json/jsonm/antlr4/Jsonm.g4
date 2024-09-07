grammar Jsonm;
// Lexer rules
LINE_COMMENT: '//' .*? '\r'? '\n' -> skip
    ;
COMMENT: '/*' .*? '*/' -> skip
    ;
WS: [ \t\r\n]+ -> skip
    ;
STRING: '"' LETTER* '"'
    ;
fragment LETTER: ESC
    | ~["\\]
    ;
fragment ESC: '\\' (["\\/bfnrt] | UNICODE)
    ;
fragment UNICODE: 'u' HEX HEX HEX HEX
    ;
fragment HEX: [0-9a-fA-F]
    ;
FRACTION: '.' [0-9]+
    ;
EXP: [Ee] [+\-]? INT
    ;
INT: '0'
    | [1-9][0-9]*
    ;
REGEX: '/' (ESC | ~[/\\])+ '/'
    ;
WILDCARD: '*'
    ;
OPTCARD: '?'
    ;
NEGATION: '!'
    ;
BOOLEAN: 'false'
    | 'true'
    ;
NULL_WORD: 'null'
    ;
NUMBER_WORD: 'number'
    ;
FLOAT_WORD: 'float'
    ;
INT_WORD: 'integer'
    ;
BOOLEAN_WORD: 'boolean'
    ;
STRING_WORD: 'string'
    ;
IMPORT_WORD: 'import'
    ;
PACKAGE_WORD: 'package'
    ;
DEF_WORD: 'def'
    ;
ID: [_$a-zA-Z][_$a-zA-Z0-9]*
    ;

// json rules
json: object
    | array
    ;
object: '{' pair (',' pair)* '}'
    | '{' '}'
    ;
pair: STRING ':' value
    ;
array: '[' value (',' value)* ']'
    | '[' ']'
    ;
value: NULL_WORD
    | BOOLEAN
    | number
    | STRING
    | object
    | array
    ;
number: integer FRACTION? EXP?
    ;
integer: '-'? INT
    ;

// jsonm rules
jsonMatch: WILDCARD
    | objectMatch
    | arrayMatch
    ;
objectMatch: '{' pairMatch (',' pairMatch)* '}'
    | '{' '}'
    ;
pairMatch: keyMatch ':' valueMatch
    ;
keyMatch: WILDCARD
    | NEGATION STRING
    | STRING OPTCARD
    | STRING
    ;
arrayMatch: '[' arrayEntryMatch (',' arrayEntryMatch)* ']' sizeRange?
    | '[' ']'
    ;
sizeRange: '(' lowerBound = INT? ',' uppperBound = INT? ')'
    | '(' sizeBound = INT ')'
    ;
arrayEntryMatch: '(' valueMatch ')' op = ('?'|'+'|'*')
    | valueMatch
    ;
valueMatch: WILDCARD
    | singleValueMatch ('|' singleValueMatch)*
    ;
singleValueMatch: NULL_WORD
    | NUMBER_WORD numberRange?
    | FLOAT_WORD numberRange?
    | INT_WORD intRange?
    | BOOLEAN_WORD
    | STRING_WORD
    | BOOLEAN
    | number
    | STRING
    | REGEX
    | objectMatch
    | arrayMatch
    ;
numberRange: openChar = ('(' | '[') lowerBound = number? ',' uppperBound = number? closeChar = (')' | ']')
    ;
intRange: openChar = ('(' | '[') lowerBound = integer? ',' uppperBound = integer? closeChar = (')' | ']')
    ;