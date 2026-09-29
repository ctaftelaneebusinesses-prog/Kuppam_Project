from rest_framework.views import exception_handler as drf_exception_handler


#: DRF's default status->name mapping is close to what we want but not
#: identical to Django's own IntegrityError/ValidationError style; this is
#: the one place every API error's machine-readable `code` comes from, so
#: a client can switch on it instead of parsing `message` text.
_CODE_BY_STATUS = {
    400: 'invalid_request',
    401: 'authentication_required',
    403: 'permission_denied',
    404: 'not_found',
    405: 'method_not_allowed',
    406: 'not_acceptable',
    409: 'conflict',
    415: 'unsupported_media_type',
    429: 'rate_limited',
    500: 'server_error',
    503: 'service_unavailable',
}

#: Codes a view/permission may set deliberately (e.g. PermissionDenied(...,
#: code='consent_required')) that a client must be able to tell apart from a
#: generic denial. Anything else keeps the status-derived code above.
_PASSTHROUGH_CODES = {'consent_required'}


def exception_handler(exc, context):
    """
    Wraps DRF's default exception handling so every error response — auth
    failures, permission denials, 404s, validation errors, throttling —
    comes back in the same shape: {"error": {"code", "message", "details"}}.
    A plain string/list of errors becomes `message`; a dict of field errors
    (serializer/form validation) becomes `details` with a generic `message`.
    """
    response = drf_exception_handler(exc, context)
    if response is None:
        return None

    data = response.data
    details = None
    if isinstance(data, dict) and 'detail' in data and len(data) == 1:
        message = str(data['detail'])
    elif isinstance(data, dict):
        details = data
        message = 'One or more fields are invalid.'
    elif isinstance(data, list):
        message = ' '.join(str(item) for item in data)
    else:
        message = str(data)

    exc_code = exc.get_codes() if hasattr(exc, 'get_codes') else None
    code = exc_code if isinstance(exc_code, str) and exc_code in _PASSTHROUGH_CODES else _CODE_BY_STATUS.get(response.status_code, 'error')

    response.data = {
        'error': {
            'code': code,
            'message': message,
            **({'details': details} if details else {}),
        }
    }
    return response
