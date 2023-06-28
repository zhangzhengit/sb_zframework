package com.vo.test;

import com.vo.anno.ZComponent;

@ZComponent
public class IS {


	@ZTest
	public void test() {
		System.out
		.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "IS.test()");

	}

	@ZTest
	public void test2() {
		System.out
				.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "IS.test2()");

	}
	public void testOK() {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "IS.testOK()");

	}

}
