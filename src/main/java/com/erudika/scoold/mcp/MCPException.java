/*
 * Copyright 2013-2026 Erudika. https://erudika.com
 *
 * Licensed under the EULA - use is subject to license terms.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika/scoold-pro
 */
package com.erudika.scoold.mcp;

/**
 * MCP error wrapper.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class MCPException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Error type.
	 */
	private final ErrorType type;
	/**
	 * Error message.
	 */
	private final String userMessage;

	/**
	 * Generic MCP exception.
	 * @param type error enum type
	 * @param userMessage error message
	 */
	MCPException(ErrorType type, String userMessage) {
		super(userMessage);
		this.type = type;
		this.userMessage = userMessage;
	}

	/**
	 * Generic MCP exception.
	 * @param type error enum type
	 * @param userMessage error message
	 * @param cause cause
	 */
	MCPException(ErrorType type, String userMessage, Throwable cause) {
		super(userMessage, cause);
		this.type = type;
		this.userMessage = userMessage;
	}

	/**
	 * Returns the type of error.
	 * @return error type
	 */
	ErrorType getType() {
		return type;
	}

	/**
	 * Returns error message.
	 * @return error message
	 */
	String getUserMessage() {
		return userMessage;
	}

	/**
	 *  Enum for error type.
	 */
	enum ErrorType {
		/**
		 * Invalid input from the MCP client (maps to 400-class errors).
		 */
		INVALID_INPUT,
		/**
		 * Resource or entity not found (maps to 404).
		 */
		NOT_FOUND,
		/**
		 * Server-side failure (maps to 500-class errors).
		 */
		INTERNAL_ERROR
	}
}
