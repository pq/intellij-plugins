abstract class I {
  get foo;
  set foo(x);
}
class A implements I {
  get foo => 0;

  @override
  set foo(x) {
    
  }

}

main() {
  new A();
}