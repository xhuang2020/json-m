grammar Jsonm;
// json syntax
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
number: '-'? INT FRACTION? EXP?
    ;
BOOLEAN: 'false'
    | 'true'
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
INT: '0'
    | [1-9][0-9]*
    ;
FRACTION: '.' [0-9]+
    ;
EXP: [Ee] [+\-]? INT
    ;
WS: [ \t\r\n]+ -> skip
    ;

// jsonm syntax
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
    | STRING OPTCARD
    | STRING
    ;
arrayMatch: '[' arrayEntryMatch (',' arrayEntryMatch)* ']' sizeRange?
    | '[' ']'
    ;
sizeRange: '(' INT? ',' INT? ')'
    | '(' INT ')'
    ;
arrayEntryMatch: '(' valueMatch ')' op = ('?'|'+'|'*')
    | valueMatch
    ;
valueMatch: WILDCARD
    | singleValueMatch ('|' singleValueMatch)*
    ;
singleValueMatch: NULL_WORD
    | NUMBER_WORD
    | INT_WORD
    | BOOLEAN_WORD
    | BOOLEAN
    | number
    | STRING
    | REGEX
    | objectMatch
    | arrayMatch
    ;
REGEX: '/' (ESC | ~[/\\])+ '/'
    ;
WILDCARD: '*'
    ;
OPTCARD: '?'
    ;
NULL_WORD: 'null'
    ;
NUMBER_WORD: 'number'
    ;
INT_WORD: 'int'
    ;
BOOLEAN_WORD: 'boolean'
    ;