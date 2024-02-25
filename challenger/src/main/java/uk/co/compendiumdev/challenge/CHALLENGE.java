package uk.co.compendiumdev.challenge;

public enum CHALLENGE {
    GET_CHALLENGES,
    GET_TODOS,
    GET_TODO,
    GET_TODO_404,
    POST_TODOS,
    POST_UPDATE_TODO,
    POST_TODOS_BAD_DONE_STATUS,
    DELETE_A_TODO,
    DELETE_ALL_TODOS,
    GET_TODOS_FILTERED,
    GET_TODOS_NOT_PLURAL_404,
    OPTIONS_TODOS,
    GET_HEAD_TODOS,
    POST_TODOS_415,
    GET_ACCEPT_XML,
    GET_ACCEPT_JSON,
    GET_ACCEPT_ANY_DEFAULT_JSON,
    GET_ACCEPT_XML_PREFERRED,
    GET_JSON_BY_DEFAULT_NO_ACCEPT,
    GET_UNSUPPORTED_ACCEPT_406,
    POST_CREATE_XML,
    POST_CREATE_JSON,
    GET_HEARTBEAT_204,
    DELETE_HEARTBEAT_405,
    POST_CREATE_JSON_ACCEPT_XML,
    POST_CREATE_XML_ACCEPT_JSON,
    TRACE_HEARTBEAT_501,
    PATCH_HEARTBEAT_500,
    CREATE_SECRET_TOKEN_401,
    CREATE_SECRET_TOKEN_201,
    GET_SECRET_NOTE_401,
    GET_SECRET_NOTE_403,
    POST_SECRET_NOTE_403,
    POST_SECRET_NOTE_401,
    POST_SECRET_NOTE_200,
    GET_SECRET_NOTE_200,
    CREATE_NEW_CHALLENGER,
    GET_SECRET_NOTE_BEARER_200,
    POST_SECRET_NOTE_BEARER_200,
    POST_ALL_TODOS,
    POST_TODOS_TOO_LONG_TITLE_LENGTH,
    POST_TODOS_TOO_LONG_DESCRIPTION_LENGTH,
    GET_RESTORE_EXISTING_CHALLENGER,
    POST_RESTORE_EXISTING_CHALLENGER,
    POST_TODOS_TOO_LONG_PAYLOAD_SIZE,
    POST_TODOS_INVALID_EXTRA_FIELD,
    POST_MAX_OUT_TITILE_DESCRIPTION_LENGTH,
    PUT_TODOS_400,
    POST_TODOS_404,
    PUT_TODOS_FULL_200,
    PUT_TODOS_PARTIAL_200, PUT_TODOS_MISSING_TITLE_400, PUT_TODOS_400_NO_AMEND_ID;
}
