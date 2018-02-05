// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The SFC licenses this file
// to you under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "Script.h"

#include "errorcodes.h"
#include "logging.h"

#include "AsyncScriptExecutor.h"
#include "Element.h"
#include "IECommandExecutor.h"
#include "VariantUtilities.h"
#include "StringUtilities.h"

namespace webdriver {

Script::Script(IHTMLDocument2* document,
               std::string script_source,
               unsigned long argument_count) {
  std::wstring wide_script = StringUtilities::ToWString(script_source);
  this->Initialize(document, wide_script, argument_count);
}

Script::Script(IHTMLDocument2* document,
               std::wstring script_source,
               unsigned long argument_count) {
  this->Initialize(document, script_source, argument_count);
}

Script::Script(IHTMLDocument2* document,
               std::string script_source) {
  std::wstring wide_script = StringUtilities::ToWString(script_source);
  this->Initialize(document, wide_script, 0);
}

Script::Script(IHTMLDocument2* document,
               std::wstring script_source) {
  this->Initialize(document, script_source, 0);
}

Script::~Script(void) {
}

void Script::Initialize(IHTMLDocument2* document,
                        const std::wstring& script_source,
                        const unsigned long argument_count) {
  LOG(TRACE) << "Entering Script::Initialize";

  this->script_engine_host_ = document;
  this->source_code_ = script_source;
  this->argument_count_ = argument_count;
  this->current_arg_index_ = 0;

  // Calling vector::resize() is okay here, because the vector
  // should be empty when Initialize() is called, and the
  // reallocation of variants shouldn't give us too much of a
  // negative impact.
  this->argument_array_.resize(this->argument_count_);
}

void Script::AddArgument(const std::string& argument) {
  LOG(TRACE) << "Entering Script::AddArgument(std::string)";
  std::wstring wide_argument = StringUtilities::ToWString(argument);
  this->AddArgument(wide_argument);
}

void Script::AddArgument(const std::wstring& argument) {
  LOG(TRACE) << "Entering Script::AddArgument(std::wstring)";
  CComVariant dest_argument(argument.c_str());
  this->AddArgument(dest_argument);
}

void Script::AddArgument(const int argument) {
  LOG(TRACE) << "Entering Script::AddArgument(int)";
  CComVariant dest_argument((long)argument);
  this->AddArgument(dest_argument);
}

void Script::AddArgument(const double argument) {
  LOG(TRACE) << "Entering Script::AddArgument(double)";
  CComVariant dest_argument(argument);
  this->AddArgument(dest_argument);
}

void Script::AddArgument(const bool argument) {
  LOG(TRACE) << "Entering Script::AddArgument(bool)";
  CComVariant dest_argument(argument);
  this->AddArgument(dest_argument);
}

void Script::AddArgument(ElementHandle argument) {
  LOG(TRACE) << "Entering Script::AddArgument(ElementHandle)";
  this->AddArgument(argument->element());
}

void Script::AddArgument(IHTMLElement* argument) {
  LOG(TRACE) << "Entering Script::AddArgument(IHTMLElement*)";
  CComVariant dest_argument(argument);
  this->AddArgument(dest_argument);
}

void Script::AddArgument(VARIANT argument) {
  LOG(TRACE) << "Entering Script::AddArgument(VARIANT)";
  CComVariant wrapped_argument(argument);
  this->argument_array_[this->current_arg_index_] = wrapped_argument;
  ++this->current_arg_index_;
}

void Script::AddNullArgument() {
  LOG(TRACE) << "Entering Script::AddNullArgument";
  CComVariant null_arg;
  null_arg.vt = VT_NULL;
  this->AddArgument(null_arg);
}

bool Script::ResultIsString() {
  LOG(TRACE) << "Entering Script::ResultIsString";
  return VariantUtilities::VariantIsString(this->result_);
}

bool Script::ResultIsInteger() {
  LOG(TRACE) << "Entering Script::ResultIsInteger";
  return VariantUtilities::VariantIsInteger(this->result_);
}

bool Script::ResultIsDouble() {
  LOG(TRACE) << "Entering Script::ResultIsDouble";
  return VariantUtilities::VariantIsDouble(this->result_);
}

bool Script::ResultIsBoolean() {
  LOG(TRACE) << "Entering Script::ResultIsBoolean";
  return VariantUtilities::VariantIsBoolean(this->result_);
}

bool Script::ResultIsEmpty() {
  LOG(TRACE) << "Entering Script::ResultIsEmpty";
  return VariantUtilities::VariantIsEmpty(this->result_);
}

bool Script::ResultIsIDispatch() {
  LOG(TRACE) << "Entering Script::ResultIsIDispatch";
  return VariantUtilities::VariantIsIDispatch(this->result_);
}

bool Script::ResultIsElementCollection() {
  LOG(TRACE) << "Entering Script::ResultIsElementCollection";
  return VariantUtilities::VariantIsElementCollection(this->result_);
}

bool Script::ResultIsElement() {
  LOG(TRACE) << "Entering Script::ResultIsElement";
  return VariantUtilities::VariantIsElement(this->result_);
}

bool Script::ResultIsArray() {
  LOG(TRACE) << "Entering Script::ResultIsArray";
  return VariantUtilities::VariantIsArray(this->result_);
}

bool Script::ResultIsObject() {
  LOG(TRACE) << "Entering Script::ResultIsObject";
  return VariantUtilities::VariantIsObject(this->result_);
}

int Script::Execute() {
  LOG(TRACE) << "Entering Script::Execute";

  CComVariant result = L"";
  CComBSTR error_description = L"";

  if (this->script_engine_host_ == NULL) {
    LOG(WARN) << "Script engine host is NULL";
    return ENOSUCHDOCUMENT;
  }
  CComVariant temp_function;
  if (!this->CreateAnonymousFunction(&temp_function)) {
    LOG(WARN) << "Cannot create anonymous function";
    return EUNEXPECTEDJSERROR;
  }

  if (temp_function.vt != VT_DISPATCH) {
    LOG(DEBUG) << "No return value that we care about";
    return WD_SUCCESS;
  }

  // Grab the "call" method out of the returned function
  DISPID call_member_id;
  OLECHAR FAR* call_member_name = L"call";
  HRESULT hr = temp_function.pdispVal->GetIDsOfNames(IID_NULL,
                                                     &call_member_name,
                                                     1,
                                                     LOCALE_USER_DEFAULT,
                                                     &call_member_id);
  if (FAILED(hr)) {
    LOGHR(WARN, hr) << "Cannot locate call method on anonymous function";
    return EUNEXPECTEDJSERROR;
  }

  CComPtr<IHTMLWindow2> win;
  hr = this->script_engine_host_->get_parentWindow(&win);
  if (FAILED(hr)) {
    LOGHR(WARN, hr) << "Cannot get parent window, IHTMLDocument2::get_parentWindow failed";
    return EUNEXPECTEDJSERROR;
  }

  // IDispatch::Invoke() expects the arguments to be passed into it
  // in reverse order. To accomplish this, we create a new variant
  // array of size n + 1 where n is the number of arguments we have.
  // we copy each element of arguments_array_ into the new array in
  // reverse order, and add an extra argument, the window object,
  // to the end of the array to use as the "this" parameter for the
  // function invocation.
  size_t arg_count = this->argument_array_.size();
  std::vector<CComVariant> argument_array_copy(arg_count + 1);
  CComVariant window_variant(win);
  argument_array_copy[arg_count].Copy(&window_variant);

  for (size_t index = 0; index < arg_count; ++index) {
    argument_array_copy[arg_count - 1 - index].Copy(&this->argument_array_[index]);
  }

  DISPPARAMS call_parameters = { 0 };
  memset(&call_parameters, 0, sizeof call_parameters);
  call_parameters.cArgs = static_cast<unsigned int>(argument_array_copy.size());
  call_parameters.rgvarg = &argument_array_copy[0];

  int return_code = WD_SUCCESS;
  EXCEPINFO exception;
  memset(&exception, 0, sizeof exception);
  hr = temp_function.pdispVal->Invoke(call_member_id,
                                      IID_NULL,
                                      LOCALE_USER_DEFAULT,
                                      DISPATCH_METHOD,
                                      &call_parameters, 
                                      &result,
                                      &exception,
                                      0);

  if (FAILED(hr)) {
    if (DISP_E_EXCEPTION == hr) {
      error_description = exception.bstrDescription ? exception.bstrDescription : L"EUNEXPECTEDJSERROR";
      CComBSTR error_source(exception.bstrSource ? exception.bstrSource : L"EUNEXPECTEDJSERROR");
      LOG(INFO) << "Exception message was: '" << error_description << "'";
      LOG(INFO) << "Exception source was: '" << error_source << "'";
    } else {
      LOGHR(DEBUG, hr) << "Failed to execute anonymous function, no exception information retrieved";
    }

    result.Clear();
    result.vt = VT_BSTR;
    result.bstrVal = error_description;
    return_code = EUNEXPECTEDJSERROR;
  }

  this->result_.Copy(&result);

  return return_code;
}

int Script::ExecuteAsync(int timeout_in_milliseconds) {
  LOG(TRACE) << "Entering Script::ExecuteAsync";
  int return_code = WD_SUCCESS;

  CComVariant result = L"";
  CComBSTR error_description = L"";

  AsyncScriptExecutorThreadContext thread_context;
  thread_context.script_source = this->source_code_.c_str();
  thread_context.script_argument_count = this->argument_count_;

  // We need exclusive access to this event. If it's already created,
  // OpenEvent returns non-NULL, so we need to wait a bit and retry
  // until OpenEvent returns NULL.
  int retry_counter = 50;
  HANDLE event_handle = ::OpenEvent(SYNCHRONIZE, FALSE, ASYNC_SCRIPT_EVENT_NAME);
  while (event_handle != NULL && --retry_counter > 0) {
    ::CloseHandle(event_handle);
    ::Sleep(50);
    event_handle = ::OpenEvent(SYNCHRONIZE, FALSE, ASYNC_SCRIPT_EVENT_NAME);
  }

  // Failure condition here.
  if (event_handle != NULL) {
    ::CloseHandle(event_handle);
    LOG(WARN) << "OpenEvent() returned non-NULL, event already exists.";
    result.Clear();
    result.vt = VT_BSTR;
    error_description = L"Couldn't create an event for synchronizing the creation of the thread. This generally means that you were trying to click on an option in two different instances.";
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  LOG(DEBUG) << "Creating synchronization event for new thread";
  event_handle = ::CreateEvent(NULL, TRUE, FALSE, ASYNC_SCRIPT_EVENT_NAME);
  if (event_handle == NULL || ::GetLastError() == ERROR_ALREADY_EXISTS) {
    if (event_handle == NULL) {
      LOG(WARN) << "CreateEvent() failed.";
      error_description = L"Couldn't create an event for synchronizing the creation of the thread. This is an internal failure at the Windows OS level, and is generally not due to an error in the IE driver.";
    } else {
      ::CloseHandle(event_handle);
      LOG(WARN) << "Synchronization event is already created in another instance.";
      error_description = L"Couldn't create an event for synchronizing the creation of the thread. This generally means that you were trying to click on an option in multiple different instances.";
    }
    result.Clear();
    result.vt = VT_BSTR;
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  // Start the thread and wait up to 1 second to be signaled that it is ready
  // to receive messages, then close the event handle.
  LOG(DEBUG) << "Starting new thread";
  unsigned int thread_id = 0;
  HANDLE thread_handle = reinterpret_cast<HANDLE>(_beginthreadex(NULL,
                                                  0,
                                                  AsyncScriptExecutor::ThreadProc,
                                                  reinterpret_cast<void*>(&thread_context),
                                                  0,
                                                  &thread_id));

  LOG(DEBUG) << "Waiting for new thread to be ready for messages";
  DWORD event_wait_result = ::WaitForSingleObject(event_handle, 5000);
  if (event_wait_result != WAIT_OBJECT_0) {
    LOG(WARN) << "Waiting for event to be signaled returned unexpected value: " << event_wait_result;
  }
  ::CloseHandle(event_handle);

  if (thread_handle == NULL) {
    LOG(WARN) << "_beginthreadex() failed.";
    result.Clear();
    result.vt = VT_BSTR;
    error_description = L"Couldn't create the thread for executing JavaScript asynchronously.";
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  HWND executor_handle = thread_context.hwnd;

  // Marshal the document and the element to click to streams for use in another thread.
  LOG(DEBUG) << "Marshaling document to stream to send to new thread";
  LPSTREAM document_stream;
  HRESULT hr = ::CoMarshalInterThreadInterfaceInStream(IID_IHTMLDocument2, this->script_engine_host_, &document_stream);
  if (FAILED(hr)) {
    LOGHR(WARN, hr) << "CoMarshalInterfaceThreadInStream() for document failed";
    result.Clear();
    result.vt = VT_BSTR;
    error_description = L"Couldn't marshal the IHTMLDocument2 interface to a stream. This is an internal COM error.";
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  ::SendMessage(executor_handle, WD_ASYNC_SCRIPT_SET_DOCUMENT, NULL, reinterpret_cast<LPARAM>(document_stream));
  for (size_t index = 0; index < this->argument_array_.size(); ++index) {
    CComVariant arg = this->argument_array_[index];
    WPARAM wparam = static_cast<WPARAM>(arg.vt);
    LPARAM lparam = NULL;
    switch (arg.vt) {
      case VT_DISPATCH: {
        LPSTREAM dispatch_stream;
        hr = ::CoMarshalInterThreadInterfaceInStream(IID_IDispatch, arg.pdispVal, &dispatch_stream);
        if (FAILED(hr)) {
          LOGHR(WARN, hr) << "CoMarshalInterfaceThreadInStream() for IDispatch argument failed";
          result.Clear();
          result.vt = VT_BSTR;
          error_description = L"Couldn't marshal the IDispatch interface to a stream. This is an internal COM error.";
          result.bstrVal = error_description;
          this->result_.Copy(&result);
          return EUNEXPECTEDJSERROR;
        }
        lparam = reinterpret_cast<LPARAM>(dispatch_stream);
        break;
      }
      default: {
        // TODO: Marshal arguments of types other than VT_DISPATCH. At present,
        // the asynchronous execution of JavaScript is only used for Automation
        // Atoms on an element which take a single argument, an IHTMLElement
        // object, which is represented as an IDispatch. This case statement
        // will get much more complex should the need arise to execute
        // arbitrary scripts in an asynchronous manner.
      }
    }
    ::SendMessage(executor_handle, WD_ASYNC_SCRIPT_SET_ARGUMENT, wparam, lparam);
  }
  ::PostMessage(executor_handle, WD_ASYNC_SCRIPT_EXECUTE, NULL, NULL);
  // We will wait a short bit and poll for the execution of the script to be
  // complete. This will allow us to say synchronous for short-running scripts
  // like clearing an input element, yet still be able to continue processing
  // when the script is blocked, as when an alert() window is present.
  LOG(TRACE) << "Waiting for async script execution to be complete";
  retry_counter = static_cast<int>(timeout_in_milliseconds / 10);
  bool is_execution_finished = ::SendMessage(executor_handle, WD_ASYNC_SCRIPT_IS_EXECUTION_COMPLETE, NULL, NULL) != 0;
  while(!is_execution_finished && --retry_counter > 0) {
    ::Sleep(10);
    is_execution_finished = ::SendMessage(executor_handle, WD_ASYNC_SCRIPT_IS_EXECUTION_COMPLETE, NULL, NULL) != 0;
  }

  if (is_execution_finished) {
    // TODO: Marshal the actual result from the AsyncScriptExecutor window
    // thread to this one. At present, the asynchronous execution of JavaScript
    // is only used for Automation Atoms on an element which could cause an
    // alert to appear (e.g., clear, click, or submit), and do not return any
    // return values back to the caller. In this case, the return code of the
    // execution method is sufficent. Marshaling the return will require two
    // more messages, one for determining the variant type of the return value,
    // and another for actually retrieving that value from the worker window's
    // thread.
    LOG(TRACE) << "Async script execution completed, getting result";
    int status_code = static_cast<int>(::SendMessage(executor_handle, WD_ASYNC_SCRIPT_GET_RESULT, NULL, NULL));
    return status_code;
  } else {
    LOG(TRACE) << "Async script execution not completed after timeout, detaching listener";
    ::SendMessage(executor_handle, WD_ASYNC_SCRIPT_DETACH_LISTENTER, NULL, NULL);
  }
  return WD_SUCCESS;
}

int Script::BeginAsyncExecution(HWND* async_executor_handle) {
  LOG(TRACE) << "Entering Script::BeginAsyncExecution";
  int return_code = WD_SUCCESS;

  CComVariant result = L"";
  CComBSTR error_description = L"";

  AsyncScriptExecutorThreadContext thread_context;
  thread_context.script_source = this->source_code_.c_str();
  thread_context.script_argument_count = this->argument_count_;

  // We need exclusive access to this event. If it's already created,
  // OpenEvent returns non-NULL, so we need to wait a bit and retry
  // until OpenEvent returns NULL.
  int retry_counter = 50;
  HANDLE event_handle = ::OpenEvent(SYNCHRONIZE, FALSE, ASYNC_SCRIPT_EVENT_NAME);
  while (event_handle != NULL && --retry_counter > 0) {
    ::CloseHandle(event_handle);
    ::Sleep(50);
    event_handle = ::OpenEvent(SYNCHRONIZE, FALSE, ASYNC_SCRIPT_EVENT_NAME);
  }

  // Failure condition here.
  if (event_handle != NULL) {
    ::CloseHandle(event_handle);
    LOG(WARN) << "OpenEvent() returned non-NULL, event already exists.";
    result.Clear();
    result.vt = VT_BSTR;
    error_description = L"Couldn't create an event for synchronizing the creation of the thread. This generally means that you were trying to click on an option in two different instances.";
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  LOG(DEBUG) << "Creating synchronization event for new thread";
  event_handle = ::CreateEvent(NULL, TRUE, FALSE, ASYNC_SCRIPT_EVENT_NAME);
  if (event_handle == NULL || ::GetLastError() == ERROR_ALREADY_EXISTS) {
    if (event_handle == NULL) {
      LOG(WARN) << "CreateEvent() failed.";
      error_description = L"Couldn't create an event for synchronizing the creation of the thread. This is an internal failure at the Windows OS level, and is generally not due to an error in the IE driver.";
    } else {
      ::CloseHandle(event_handle);
      LOG(WARN) << "Synchronization event is already created in another instance.";
      error_description = L"Couldn't create an event for synchronizing the creation of the thread. This generally means that you were trying to click on an option in multiple different instances.";
    }
    result.Clear();
    result.vt = VT_BSTR;
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  // Start the thread and wait up to 1 second to be signaled that it is ready
  // to receive messages, then close the event handle.
  LOG(DEBUG) << "Starting new thread";
  unsigned int thread_id = 0;
  HANDLE thread_handle = reinterpret_cast<HANDLE>(_beginthreadex(NULL,
                                                                 0,
                                                                 AsyncScriptExecutor::ThreadProc,
                                                                 reinterpret_cast<void*>(&thread_context),
                                                                 0,
                                                                 &thread_id));

  LOG(DEBUG) << "Waiting for new thread to be ready for messages";
  DWORD event_wait_result = ::WaitForSingleObject(event_handle, 5000);
  if (event_wait_result != WAIT_OBJECT_0) {
    LOG(WARN) << "Waiting for event to be signaled returned unexpected value: " << event_wait_result;
  }
  ::CloseHandle(event_handle);

  if (thread_handle == NULL) {
    LOG(WARN) << "_beginthreadex() failed.";
    result.Clear();
    result.vt = VT_BSTR;
    error_description = L"Couldn't create the thread for executing JavaScript asynchronously.";
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  HWND executor_handle = thread_context.hwnd;
  *async_executor_handle = executor_handle;

  // Marshal the document and the element to click to streams for use in another thread.
  LOG(DEBUG) << "Marshaling document to stream to send to new thread";
  LPSTREAM document_stream;
  HRESULT hr = ::CoMarshalInterThreadInterfaceInStream(IID_IHTMLDocument2, this->script_engine_host_, &document_stream);
  if (FAILED(hr)) {
    LOGHR(WARN, hr) << "CoMarshalInterfaceThreadInStream() for document failed";
    result.Clear();
    result.vt = VT_BSTR;
    error_description = L"Couldn't marshal the IHTMLDocument2 interface to a stream. This is an internal COM error.";
    result.bstrVal = error_description;
    this->result_.Copy(&result);
    return EUNEXPECTEDJSERROR;
  }

  ::SendMessage(executor_handle, WD_ASYNC_SCRIPT_SET_DOCUMENT, NULL, reinterpret_cast<LPARAM>(document_stream));
  for (size_t index = 0; index < this->argument_array_.size(); ++index) {
    CComVariant arg = this->argument_array_[index];
    WPARAM wparam = static_cast<WPARAM>(arg.vt);
    LPARAM lparam = NULL;
    switch (arg.vt) {
    case VT_DISPATCH: {
      LPSTREAM dispatch_stream;
      hr = ::CoMarshalInterThreadInterfaceInStream(IID_IDispatch, arg.pdispVal, &dispatch_stream);
      if (FAILED(hr)) {
        LOGHR(WARN, hr) << "CoMarshalInterfaceThreadInStream() for IDispatch argument failed";
        result.Clear();
        result.vt = VT_BSTR;
        error_description = L"Couldn't marshal the IDispatch interface to a stream. This is an internal COM error.";
        result.bstrVal = error_description;
        this->result_.Copy(&result);
        return EUNEXPECTEDJSERROR;
      }
      lparam = reinterpret_cast<LPARAM>(dispatch_stream);
      break;
    }
    case VT_BSTR:
      lparam = reinterpret_cast<LPARAM>(&arg.bstrVal);
      break;
    case VT_BOOL:
      lparam = static_cast<LPARAM>(arg.boolVal != VARIANT_FALSE);
      break;
    case VT_I4:
    case VT_I8:
      lparam = static_cast<LPARAM>(arg.intVal);
      break;
    case VT_R4:
    case VT_R8:
      lparam = reinterpret_cast<LPARAM>(&arg.dblVal);
      break;
    default: {
      // TODO: Marshal arguments of types other than VT_DISPATCH. At present,
      // the asynchronous execution of JavaScript is only used for Automation
      // Atoms on an element which take a single argument, an IHTMLElement
      // object, which is represented as an IDispatch. This case statement
      // will get much more complex should the need arise to execute
      // arbitrary scripts in an asynchronous manner.
      }
    }
    ::SendMessage(executor_handle, WD_ASYNC_SCRIPT_SET_ARGUMENT, wparam, lparam);
  }
  ::PostMessage(executor_handle, WD_ASYNC_SCRIPT_EXECUTE, NULL, NULL);
  return WD_SUCCESS;
}

int Script::ConvertResultToJsonValue(const IECommandExecutor& executor,
                                     Json::Value* value) {
  LOG(TRACE) << "Entering Script::ConvertResultToJsonValue";
  return this->ConvertResultToJsonValue(executor.window_handle(), value);
}

int Script::ConvertResultToJsonValue(HWND element_repository_handle,
                                     Json::Value* value) {
  LOG(TRACE) << "Entering Script::ConvertResultToJsonValue";
  return VariantUtilities::ConvertVariantToJsonValue(element_repository_handle,
                                                     this->result_,
                                                     value);
}

bool Script::ConvertResultToString(std::string* value) {
  LOG(TRACE) << "Entering Script::ConvertResultToString";
  return VariantUtilities::ConvertVariantToString(this->result_, value);
}

bool Script::CreateAnonymousFunction(VARIANT* result) {
  LOG(TRACE) << "Entering Script::CreateAnonymousFunction";

  std::wstring function_eval_script = L"window.document.__webdriver_script_fn = ";
  function_eval_script.append(this->source_code_.c_str());
  CComBSTR code(function_eval_script.c_str());
  CComBSTR lang(L"JScript");
  CComVariant exec_script_result;

  CComPtr<IHTMLWindow2> window;
  HRESULT hr = this->script_engine_host_->get_parentWindow(&window);
  if (FAILED(hr)) {
    LOGHR(WARN, hr) << "Unable to get parent window, call to IHTMLDocument2::get_parentWindow failed";
    return false;
  }

  hr = window->execScript(code, lang, &exec_script_result);
  if (FAILED(hr)) {
    LOGHR(WARN, hr) << "Unable to execute code, call to IHTMLWindow2::execScript failed";
    return false;
  }

  bool get_result_success = VariantUtilities::GetVariantObjectPropertyValue(
      this->script_engine_host_,
      L"__webdriver_script_fn",
      result);
  return get_result_success;
}

int Script::AddArguments(HWND element_repository_handle, const Json::Value& arguments) {
  LOG(TRACE) << "Entering Script::AddArguments";

  int status_code = WD_SUCCESS;

  // Calling vector::resize() is okay here, because the vector
  // should be empty when Initialize() is called, and the
  // reallocation of variants shouldn't give us too much of a
  // negative impact.
  this->argument_array_.resize(arguments.size());

  for (UINT arg_index = 0; arg_index < arguments.size(); ++arg_index) {
    Json::Value arg = arguments[arg_index];
    status_code = this->AddArgument(element_repository_handle, arg);
    if (status_code != WD_SUCCESS) {
      break;
    }
  }

  return status_code;
}

int Script::AddArgument(HWND element_repository_handle, const Json::Value& arg) {
  LOG(TRACE) << "Entering Script::AddArgument";

  int status_code = WD_SUCCESS;
  if (arg.isString()) {
    std::string value = arg.asString();
    this->AddArgument(value);
  } else if (arg.isInt()) {
    int int_number = arg.asInt();
    this->AddArgument(int_number);
  } else if (arg.isDouble()) {
    double dbl_number = arg.asDouble();
    this->AddArgument(dbl_number);
  } else if (arg.isBool()) {
    bool bool_arg = arg.asBool();
    this->AddArgument(bool_arg);
  } else if (arg.isNull()) {
    this->AddNullArgument();
  } else if (arg.isArray()) {
    status_code = this->WalkArray(element_repository_handle, arg);
  } else if (arg.isObject()) {
    std::string element_marker_property_name = "element-6066-11e4-a52e-4f735466cecf";
    if (arg.isMember(element_marker_property_name)) {
      ElementInfo info;
      info.element_id = arg[element_marker_property_name].asString();
      status_code = static_cast<int>(::SendMessage(element_repository_handle,
                                     WD_GET_MANAGED_ELEMENT,
                                     NULL,
                                     reinterpret_cast<LPARAM>(&info)));

      if (status_code == WD_SUCCESS) {
        ElementHandle wrapped_element(new Element(info.element, info.element_id));
        bool is_element_valid = wrapped_element->IsAttachedToDom();
        if (is_element_valid) {
          CComPtr<IDispatch> parent_doc_dispatch;
          wrapped_element->element()->get_document(&parent_doc_dispatch);
          is_element_valid = this->script_engine_host_.IsEqualObject(parent_doc_dispatch);
        }

        if (is_element_valid) {
          this->AddArgument(info.element);
        } else {
          status_code = EOBSOLETEELEMENT;
        }
      }
    } else {
      status_code = this->WalkObject(element_repository_handle, arg);
    }
  }

  return status_code;
}

int Script::WalkArray(HWND element_repository_handle,
                      const Json::Value& array_value) {
  LOG(TRACE) << "Entering Script::WalkArray";

  int status_code = WD_SUCCESS;
  Json::UInt array_size = array_value.size();
  std::wstring array_script = L"(function(){ return function() { return [";
  for (Json::UInt index = 0; index < array_size; ++index) {
    if (index != 0) {
      array_script += L",";
    }
    std::wstring index_string = std::to_wstring(static_cast<long long>(index));
    array_script += L"arguments[" + index_string + L"]";
  }
  array_script += L"];}})();";

  Script array_script_wrapper(this->script_engine_host_, array_script, array_size);
  for (Json::UInt index = 0; index < array_size; ++index) {
    status_code = array_script_wrapper.AddArgument(element_repository_handle, array_value[index]);
    if (status_code != WD_SUCCESS) {
      break;
    }
  }

  if (status_code == WD_SUCCESS) {
    status_code = array_script_wrapper.Execute();
  }

  if (status_code == WD_SUCCESS) {
    this->AddArgument(array_script_wrapper.result());
  }

  return status_code;
}

int Script::WalkObject(HWND element_repository_handle,
                       const Json::Value& object_value) {
  LOG(TRACE) << "Entering Script::WalkObject";

  int status_code = WD_SUCCESS;
  Json::Value::iterator it = object_value.begin();
  int counter = 0;
  std::string object_script = "(function(){ return function() { return {";
  for (; it != object_value.end(); ++it) {
    if (counter != 0) {
      object_script += ",";
    }
    std::string counter_string = std::to_string(static_cast<long long>(counter));
    std::string name = it.memberName();
    object_script += "\"" + name + "\"" + ":arguments[" + counter_string + "]";
    ++counter;
  }
  object_script += "};}})();";

  Script object_script_wrapper(this->script_engine_host_, object_script, counter);
  for (it = object_value.begin(); it != object_value.end(); ++it) {
    status_code = object_script_wrapper.AddArgument(element_repository_handle, object_value[it.memberName()]);
    if (status_code != WD_SUCCESS) {
      break;
    }
  }

  if (status_code == WD_SUCCESS) {
    status_code = object_script_wrapper.Execute();
  }

  if (status_code == WD_SUCCESS) {
    this->AddArgument(object_script_wrapper.result());
  }
  return status_code;
}

} // namespace webdriver
