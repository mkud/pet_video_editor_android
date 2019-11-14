/*
 * internal_exception.h
 *
 *  Created on: 25 окт. 2017 г.
 *      Author: maxx
 */

#ifndef INTERNAL_EXCEPTION_H_
#define INTERNAL_EXCEPTION_H_

#include <sstream>
#include <stdexcept>
#include <sys/time.h>

class MyFormatter {
public:
	MyFormatter() {
	}

	~MyFormatter() {
	}

	template<typename Type>
	MyFormatter &operator<<(const Type &value) {
		stream_ << value;
		return *this;
	}

	std::string str() const {
		return stream_.str();
	}

	operator std::string() const {
		return stream_.str();
	}

	enum ConvertToString {
		to_str
	};

	std::string operator >>(ConvertToString) {
		return stream_.str();
	}

private:
	std::stringstream stream_;

	MyFormatter(const MyFormatter &);

	MyFormatter & operator =(MyFormatter &);
};

#endif /* INTERNAL_EXCEPTION_H_ */
