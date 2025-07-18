<?php

namespace App\Modules\GmTool\Controller;

use App\Modules\GmTool\Foundation\Controller;

class SampleController extends Controller
{
    public function indexAction(): void
    {
        // 基本的な変数
        $intVar = 42;
        $floatVar = 3.14;
        $this->setVar('sum', $intVar + $floatVar);
    }

    public function testAction(): void
    {
        $this->setVar('value', 3.14);
    }
}
